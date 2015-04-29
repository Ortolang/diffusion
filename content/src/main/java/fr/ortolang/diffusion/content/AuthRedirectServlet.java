package fr.ortolang.diffusion.content;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(name="AuthRedirectServlet", urlPatterns={"/auth"}, loadOnStartup=2)
public class AuthRedirectServlet extends HttpServlet {
	
	public static final String UNAUTHORIZED_PATH_ATTRIBUTE_NAME = "unauthorizedPath";
	
	private static final Logger LOGGER = Logger.getLogger(AuthRedirectServlet.class.getName());
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.log(Level.INFO, "Received redirect request");
		if ( request.getSession().getAttribute(UNAUTHORIZED_PATH_ATTRIBUTE_NAME) != null ) {
			response.sendRedirect(request.getServletContext().getContextPath()+(String)request.getSession().getAttribute(UNAUTHORIZED_PATH_ATTRIBUTE_NAME));
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "no path found for redirection");
		}
	}

}
