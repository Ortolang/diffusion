package fr.ortolang.diffusion.admin.filter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class AuthenticationFilter implements Filter {
	
	private Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());
	
	@EJB 
	private MembershipService membership; 
	
	@Override
	public void init(FilterConfig config) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		logger.log(Level.FINE, "Authentication Filter Called");
		
		HttpServletRequest hrequest = (HttpServletRequest)request;
		String username = (String) hrequest.getSession().getAttribute("username");
		String password = (String) hrequest.getSession().getAttribute("password");
		if ( username != null ) {
			logger.log(Level.FINEST, "A session exists with username: " + username);
			hrequest.login(username, password);
			logger.log(Level.FINEST, "User logged");
			String profilekey = membership.getProfileKeyForConnectedIdentifier();
			try {
				Profile profile = membership.readProfile(profilekey);
				logger.log(Level.FINEST, "User profile loaded");
				if ( !profile.getStatus().equals(ProfileStatus.ACTIVATED) ) {
					logger.log(Level.FINEST, "Profile is not activated, loggin out and return to login page");
					hrequest.logout();
					hrequest.getSession().invalidate();
					request.setAttribute("errortitle", "Échec d'authentification");
					request.setAttribute("errormessage", "Compte utilisateur inactif, merci de contacter l'administrateur du système.");
					hrequest.getServletContext().getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(hrequest, response);
				}
				request.setAttribute("profile", profile);
			} catch (MembershipServiceException | KeyNotFoundException | AccessDeniedException e) {
				logger.log(Level.SEVERE, "Unable to load profile for user identifier: " + username);
				throw new ServletException ("unable to load user profile", e);
			}
			chain.doFilter(hrequest, response);
			hrequest.logout();
			
		} else {
			logger.log(Level.FINEST, "No session found, going to login page");
			hrequest.getServletContext().getRequestDispatcher("/WEB-INF/pages/login.jsp").forward(hrequest, response);
		}
	}

}
