package fr.ortolang.diffusion.api.filter;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
	
	private static final Logger LOGGER = Logger.getLogger(CORSFilter.class.getName());
	
	private static final String ORIGIN_PROPERTY = "Origin";
	private static final String ACCESS_CONTROL_ORIGIN_PROPERTY = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String CONTENT_TYPE_PROPERTY = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String OPTIONS_METHOD = "OPTIONS";

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		LOGGER.log(Level.FINE, "Filtering Origin");
		
		HttpServletRequest hrequest = (HttpServletRequest)request;
		String origin = hrequest.getHeader(ORIGIN_PROPERTY);
        if(origin != null && !origin.isEmpty()) {
        	LOGGER.log(Level.FINEST, "Origin found in headers : " + origin);
        	((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ORIGIN_PROPERTY, origin);
        }
        if (hrequest.getMethod().equals(OPTIONS_METHOD)) {
            ((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ALLOW_METHODS, "DELETE, PUT, HEAD, OPTIONS, TRACE, GET, POST");
            ((HttpServletResponse)response).setHeader(ACCESS_CONTROL_ALLOW_HEADERS, AUTHORIZATION_PROPERTY + ", " + CONTENT_TYPE_PROPERTY + ", " + CONTENT_TRANSFER_ENCODING + ", Range");
            ((HttpServletResponse)response).setHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Encoding, Content-Length, Content-Range");
        }
        chain.doFilter(request, response);
	}
}
