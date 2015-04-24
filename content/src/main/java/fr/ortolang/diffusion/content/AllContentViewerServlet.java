package fr.ortolang.diffusion.content;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@SuppressWarnings("serial")
public class AllContentViewerServlet extends ContentViewer {
	
	private static final Logger LOGGER = Logger.getLogger(AllContentViewerServlet.class.getName());
	
	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;
	
	@Override
	public void init() throws ServletException {
		LOGGER.log(Level.FINE, "AllContentViewerServlet Initialized");
		super.init();
	}
	
	@Override 
	protected CoreService getCoreService() {
		return core;
	} 
	
	@Override 
	protected BrowserService getBrowserService() {
		return browser;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.log(Level.INFO, "Received request URI : " + request.getRequestURI());
		try {
			PathBuilder puri = PathBuilder.fromPath(request.getPathInfo());
			String[] parts = puri.buildParts();
			
			if ( parts.length < 1 ) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing workspace alias in url");
				return;
			}
			request.setAttribute(CTX_ATTRIBUTE_NAME, request.getContextPath());
			request.setAttribute(WSALIAS_ATTRIBUTE_NAME, parts[0]);
			request.setAttribute(WSKEY_ATTRIBUTE_NAME, core.resolveWorkspaceAlias(parts[0]));
			request.setAttribute(ROOT_ATTRIBUTE_NAME, ( parts.length > 1 )?parts[1]:null);
			PathBuilder path = ( parts.length > 2 )?puri.clone().relativize("/"+parts[0]+"/"+parts[1]):PathBuilder.fromPath("/"); 
			request.setAttribute(PATH_ATTRIBUTE_NAME, path.build());
			request.setAttribute(PARENT_PATH_ATTRIBUTE_NAME, path.parent().build());
			request.setAttribute(BASE_URL_ATTRIBUTE_NAME, "/all/" + parts[0] + ((parts.length > 1)?"/"+parts[1]:""));
			
			handleContent(request, response);

		} catch (InvalidPathException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (DataNotFoundException | OrtolangException | CoreServiceException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (KeyNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
		} catch (AccessDeniedException e) {
			response.setHeader("WWW-Authenticate", "Basic");
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
		} 
	}
	
}