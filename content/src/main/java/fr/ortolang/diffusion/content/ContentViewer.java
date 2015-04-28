package fr.ortolang.diffusion.content;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.*;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public abstract class ContentViewer extends HttpServlet {
	
	public static final String WSKEY_ATTRIBUTE_NAME = "wskey";
	public static final String WSALIAS_ATTRIBUTE_NAME = "alias";
	public static final String ROOT_ATTRIBUTE_NAME = "root";
	public static final String PATH_ATTRIBUTE_NAME = "path";
	public static final String PARENT_PATH_ATTRIBUTE_NAME = "ppath";
	public static final String BASE_URL_ATTRIBUTE_NAME = "base";
	public static final String CTX_ATTRIBUTE_NAME = "ctx";
	public static final String ELEMENTS_ATTRIBUTE_NAME = "elements";
	public static final String ASC_ATTRIBUTE_NAME = "asc";
	public static final String SORT_ATTRIBUTE_NAME = "sort";
	
	
	
	protected abstract CoreService getCoreService();
	
	protected abstract BrowserService getBrowserService();
	
	protected void handleContent(HttpServletRequest request, HttpServletResponse response) throws InvalidPathException, CoreServiceException, KeyNotFoundException, ServletException, IOException, OrtolangException, DataNotFoundException {
		try {
			if ( request.getAttribute(ROOT_ATTRIBUTE_NAME) == null ) {
				List<CollectionElement> elements = loadWorkspaceCatalog((String)request.getAttribute(WSKEY_ATTRIBUTE_NAME));
				request.setAttribute("elements", elements);
				request.getRequestDispatcher("/jsp/collection.jsp").forward(request, response);
				return;
			} else {
				String key = getCoreService().resolveWorkspacePath((String)request.getAttribute(WSKEY_ATTRIBUTE_NAME), (String)request.getAttribute(ROOT_ATTRIBUTE_NAME), (String)request.getAttribute(PATH_ATTRIBUTE_NAME));
				OrtolangObject object = getBrowserService().findObject(key);
				response.setStatus(HttpServletResponse.SC_OK);
				if ( object instanceof DataObject ) {
					response.setContentLength((int)((DataObject)object).getSize());
					response.setContentType(((DataObject)object).getMimeType());
					InputStream input = getCoreService().download(key);
					try {
						IOUtils.copy(input, response.getOutputStream());
					} finally {
						IOUtils.closeQuietly(input);
					}
					return;
				}
				if ( object instanceof Collection ) {
					request.setAttribute("parent", PathBuilder.fromPath((String)request.getAttribute(PATH_ATTRIBUTE_NAME)).parent().build());
					String sort = "N";
					boolean asc = true;
					if ( request.getParameter("C") != null && request.getParameter("C").matches("[MST]") ) {
						sort = request.getParameter("C");
					}
					if ( request.getParameter("O") != null && request.getParameter("O").equals("D") ) {
						asc = false;
					}
					List<CollectionElement> elements = loadCollectionCatalog((Collection)object, sort, asc);
					request.setAttribute("elements", elements);
					request.setAttribute("asc", asc);
					request.setAttribute("sort", sort);
					request.getRequestDispatcher("/jsp/collection.jsp").forward(request, response);
					return;
				}
			} 
		} catch ( AccessDeniedException e ) {
			response.setHeader("WWW-Authenticate", "Basic");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
		}
	}

	private List<CollectionElement> loadWorkspaceCatalog(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		Workspace workspace = getCoreService().readWorkspace(wskey);
		List<CollectionElement> elements = new ArrayList<CollectionElement> ();
		for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
			CollectionElement element = new CollectionElement(Collection.OBJECT_TYPE, snapshot.getName(), 0, 0, Collection.MIME_TYPE, snapshot.getKey());
			elements.add(element);
		}
		CollectionElement head = new CollectionElement(Collection.OBJECT_TYPE, Workspace.HEAD, 0, 0, Collection.MIME_TYPE, workspace.getHead());
		elements.add(head);
		Collections.sort(elements);
		return elements;
	}
	
	private List<CollectionElement> loadCollectionCatalog(Collection collection, String sort, boolean asc) {
		List<CollectionElement> elements = new ArrayList<CollectionElement> (collection.getElements());
		switch ( sort ) {
			case "M" :
				if ( asc ) {
					Collections.sort(elements, CollectionElement.ElementDateAscComparator);
				} else {
					Collections.sort(elements, CollectionElement.ElementDateDescComparator);
				};
				break;
			case "S" :
				if ( asc ) {
					Collections.sort(elements, CollectionElement.ElementSizeAscComparator);
				} else {
					Collections.sort(elements, CollectionElement.ElementSizeDescComparator);
				};
			case "T" :
				if ( asc ) {
					Collections.sort(elements, CollectionElement.ElementTypeAscComparator);
				} else {
					Collections.sort(elements, CollectionElement.ElementTypeDescComparator);
				};
			default :
				if ( asc ) {
					Collections.sort(elements, CollectionElement.ElementNameAscComparator);
				} else {
					Collections.sort(elements, CollectionElement.ElementNameDescComparator);
				};
		}
		return elements;
	}

}
