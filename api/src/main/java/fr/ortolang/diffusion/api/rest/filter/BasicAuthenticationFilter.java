package fr.ortolang.diffusion.api.rest.filter;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

public class BasicAuthenticationFilter implements Filter {
	
	private Logger logger = Logger.getLogger(BasicAuthenticationFilter.class.getName());
	
	private static final String AUTHORIZATION_PROPERTY = "Authorization";
	private static final String AUTHENTICATION_SCHEME = "Basic";
    private static final String OPTIONS_METHOD = "OPTIONS";

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		logger.log(Level.FINE, "Filtering Basic Authentication");

		HttpServletRequest hrequest = (HttpServletRequest)request;
        if (hrequest.getMethod().equals(OPTIONS_METHOD)) {
            logger.log(Level.FINEST, "Request method is OPTIONS; skipping authentication");
            chain.doFilter(request, response);
            return;
        }
		String authorization = hrequest.getHeader(AUTHORIZATION_PROPERTY);
		if ( hrequest.getUserPrincipal() != null ) {
			logger.log(Level.FINEST, "User principal is not null, maybe to a missing logout due to previous exception, forcing logout...");
			hrequest.logout();
		}
        if(authorization == null || authorization.isEmpty()) {
        	logger.log(Level.FINEST, "No authentication found in headers : guest login");
        	hrequest.login("guest", "tagada");
        	chain.doFilter(request, response);
        	hrequest.logout();
        } else {
        	logger.log(Level.FINEST, "Authentication headers founds : parsing credentials and performing user login");
			String encodedUserPassword = authorization.replaceFirst(AUTHENTICATION_SCHEME + " ", "");
			String usernameAndPassword = new String(Base64.decodeBase64(encodedUserPassword));
			StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
			String username = tokenizer.nextToken();
			String password = tokenizer.nextToken();
			logger.log(Level.FINEST, "received credentials : " + username + "/" + password.replaceAll(".*", "*"));
			hrequest.login(username, password);
			chain.doFilter(request, response);
			hrequest.logout();
        }
	}
	

}
