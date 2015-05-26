package fr.ortolang.diffusion.content;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.preview.PreviewService;
import fr.ortolang.diffusion.preview.PreviewServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@SuppressWarnings("serial")
@WebServlet(name="KeyViewerServlet", urlPatterns={"/key/*"}, loadOnStartup=2)
public class KeyViewerServlet extends ContentViewer {
	
	private static final Logger LOGGER = Logger.getLogger(KeyViewerServlet.class.getName());
	
	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;
	@EJB
	private PreviewService preview;
	@EJB
	private BinaryStoreService binary;
	
	@Override
	public void init() throws ServletException {
		LOGGER.log(Level.FINE, "KeyViewerServlet Initialized");
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
	protected PreviewService getPreviewService() {
		return preview;
	}
	
	@Override 
	protected BinaryStoreService getBinaryStoreService() {
		return binary;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.log(Level.INFO, "Received request URI : " + request.getRequestURI());
		try {
			PathBuilder puri = PathBuilder.fromPath(request.getPathInfo());
			String[] parts = puri.buildParts();
			
			if ( parts.length < 1 ) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing key in url");
				return;
			}
			request.setAttribute(CTX_ATTRIBUTE_NAME, request.getContextPath());
			request.setAttribute(OBJECTKEY_ATTRIBUTE_NAME, parts[0]);
			request.setAttribute(BASE_URL_ATTRIBUTE_NAME, "/key/");
			
			handleContent(request, response);

		} catch (InvalidPathException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (DataNotFoundException | OrtolangException | CoreServiceException | PreviewServiceException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (KeyNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
		} catch (AccessDeniedException e) {
			request.getSession().setAttribute(AuthRedirectServlet.UNAUTHORIZED_PATH_ATTRIBUTE_NAME, request.getAttribute(BASE_URL_ATTRIBUTE_NAME) + request.getPathInfo());
			response.sendRedirect(request.getServletContext().getContextPath()+"/auth");
		}
	}
	
}
