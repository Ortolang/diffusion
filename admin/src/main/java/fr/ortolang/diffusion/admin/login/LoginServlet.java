package fr.ortolang.diffusion.admin.login;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.ortolang.diffusion.membership.MembershipService;

@SuppressWarnings("serial")
public class LoginServlet extends HttpServlet {
	
	private Logger logger = Logger.getLogger(LoginServlet.class.getName());
	
	@EJB 
	private MembershipService membership; 
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.log(Level.FINE, "Login Servlet Called");
		String action = request.getParameter("action"); 
		if ( action != null && action.equals("login") ) {
			logger.log(Level.FINEST, "login action asked");
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			logger.log(Level.FINEST, "try to login with provided credentials");
			request.login(username, password);
			String profilekey = membership.getProfileKeyForConnectedIdentifier();
			if ( profilekey.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				logger.log(Level.FINEST, "credentials invalid, connected identifier is: " + MembershipService.UNAUTHENTIFIED_IDENTIFIER);
				request.logout();
				request.getSession().invalidate();
				request.setAttribute("errortitle", "Échec d'authentification");
				request.setAttribute("errormessage", "Identifiant ou mot de passe incorrect");
				request.getServletContext().getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
			} else {
				//TODO maybe check if user belongs to admin group...
				logger.log(Level.FINEST, "credentials are VALID, setting session attributes accordingly and redirecting to home");
				request.getSession().setAttribute("username", username);
				request.getSession().setAttribute("password", password);
				request.logout();
				response.sendRedirect("home.sdo");
			}
		}
		if ( action != null && action.equals("logout") ) {
			logger.log(Level.FINEST, "logout action asked");
			request.logout();
			request.getSession().invalidate();
		}
		getServletContext().getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(request, response);
	}

}
