package fr.ortolang.diffusion.api.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@SuppressWarnings("serial")
public class HtmlViewerServlet extends HttpServlet {
	
	private static final Logger LOGGER = Logger.getLogger(HtmlViewerServlet.class.getName());
	public static final String SERVLET_MAPPING = "/html";
	
	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;

	@Override
	public void init() throws ServletException {
		LOGGER.log(Level.FINE, "HtmlViewerServlet Initialized");
		super.init();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.log(Level.INFO, "Received request URI : " + request.getRequestURI());
		try {
			PathBuilder puri = PathBuilder.fromPath(request.getPathInfo());
			String[] parts = puri.buildParts();
			if ( puri.depth() == 1 ) {
				String wskey = core.resolveWorkspaceAlias(parts[0]);
				Workspace workspace = core.readWorkspace(wskey);
				List<CollectionElement> elements = new ArrayList<CollectionElement> ();
				for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
					CollectionElement element = new CollectionElement(Collection.OBJECT_TYPE, snapshot.getName(), 0, 0, Collection.MIME_TYPE, snapshot.getKey());
					elements.add(element);
				}
				CollectionElement head = new CollectionElement(Collection.OBJECT_TYPE, "head", 0, 0, Collection.MIME_TYPE, workspace.getHead());
				elements.add(head);
				Collections.sort(elements);
				request.setAttribute("path", puri.build());
				request.setAttribute("ctx", request.getContextPath());
				request.setAttribute("elements", elements);
				request.getRequestDispatcher("/jsp/collection.jsp").forward(request, response);
			} else if ( puri.depth() > 1 ) {
				String wskey = core.resolveWorkspaceAlias(parts[0]);
				String root = parts[1];
				PathBuilder path = PathBuilder.fromPath(request.getPathInfo().replaceFirst("/" + parts[0] + "/" + parts[1], ""));
				String key = core.resolveWorkspacePath(wskey, root, path.build());
				
				OrtolangObject object = browser.findObject(key);
				response.setStatus(HttpServletResponse.SC_OK);
				if ( object instanceof DataObject ) {
					response.setContentLength((int)((DataObject)object).getSize());
					response.setContentType(((DataObject)object).getMimeType());
					InputStream input = core.download(key);
					try {
						IOUtils.copy(input, response.getOutputStream());
					} finally {
						IOUtils.closeQuietly(input);
					}
					return;
				}
				if ( object instanceof Collection ) {
					request.setAttribute("path", puri.build());
					request.setAttribute("ctx", request.getContextPath());
					request.setAttribute("parent", puri.clone().parent().build());
					List<CollectionElement> elements = new ArrayList<CollectionElement> (((Collection) object).getElements());
					Collections.sort(elements);
					request.setAttribute("elements", elements);
					request.getRequestDispatcher("/jsp/collection.jsp").forward(request, response);
				}
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you have to provide a workspace alias in order to view its content");
			}
		} catch (InvalidPathException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (DataNotFoundException | OrtolangException | CoreServiceException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (KeyNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
		} catch (AccessDeniedException e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
		} 
		
		
		//1. Parse URL
		//2. Load workspace by alias for first segment
		//3. Find snapshot
		//4. Load path from snapshot
		//5. Generate neither an html view for a collection, a redirect for a link, a download for an object
	}

}
