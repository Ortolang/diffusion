package fr.ortolang.diffusion.rest.security;

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

public class OriginFilter implements Filter {
	
	private Logger logger = Logger.getLogger(OriginFilter.class.getName());
	
	private static final String ORIGIN_PROPERTY = "Origin";
	private static final String ACCESS_CONTROL_ORIGIN_PROPERTY = "Access-Control-Allow-Origin";

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		logger.log(Level.INFO, "Filtering Origin");
		
		HttpServletRequest hrequest = (HttpServletRequest)request;
		String origin = hrequest.getHeader(ORIGIN_PROPERTY);
        if(origin != null && !origin.isEmpty()) {
        	logger.log(Level.INFO, "Origin found in headers : " + origin);
        	((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ORIGIN_PROPERTY, origin);
        } 
        chain.doFilter(request, response);
	}
}
