package fr.ortolang.diffusion.client.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;

@Provider
public class CookieFilter implements ClientRequestFilter, ClientResponseFilter {

	private static final Logger logger = Logger.getLogger(CookieFilter.class.getName());
	private static List<Object> cookies = new ArrayList<Object>();

	public CookieFilter() {
	}

	@Override
	public void filter(ClientRequestContext request) throws IOException {
		logger.log(Level.FINE, "Injecting Cookies in request headers");
		request.getHeaders().put("Cookie", cookies);
	}

	@Override
	public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {
		logger.log(Level.FINE, "Storing cookies");
		for ( Entry<String, NewCookie> entry : response.getCookies().entrySet() ) {
			Cookie cookie = entry.getValue().toCookie(); 
			cookies.add(cookie);
			logger.log(Level.INFO, "New Cookie stored : " + cookie);
		}
	}
}