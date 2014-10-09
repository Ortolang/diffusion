package fr.ortolang.diffusion.api.rest.filter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CORSFilter implements Filter {
	
	private Logger logger = Logger.getLogger(CORSFilter.class.getName());
	
	private static final String ORIGIN_PROPERTY = "Origin";
	private static final String ACCESS_CONTROL_ORIGIN_PROPERTY = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String CONTENT_TYPE_PROPERTY = "Content-Type";
    private static final String OPTIONS_METHOD = "OPTIONS";

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		logger.log(Level.FINE, "Filtering Origin");
		
		HttpServletRequest hrequest = (HttpServletRequest)request;
		String origin = hrequest.getHeader(ORIGIN_PROPERTY);
        if(origin != null && !origin.isEmpty()) {
        	logger.log(Level.INFO, "Origin found in headers : " + origin);
//        	((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ORIGIN_PROPERTY, origin);
            // TODO remove dev hack allowing Cross-origin
        	((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ORIGIN_PROPERTY, "*");
        }
        if (hrequest.getMethod().equals(OPTIONS_METHOD)) {
            ((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ALLOW_METHODS, "DELETE, PUT, HEAD, OPTIONS, TRACE, GET, POST");
            ((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ALLOW_HEADERS, AUTHORIZATION_PROPERTY + ", " + CONTENT_TYPE_PROPERTY);
        }
        chain.doFilter(request, response);
	}
}
