package fr.ortolang.diffusion.api.rest;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import fr.ortolang.diffusion.OrtolangConfig;

public class DiffusionUriBuilder {
	
	public static UriBuilder getRestUriBuilder() {
		try {
			StringBuffer path = new StringBuffer();
			if ( OrtolangConfig.getInstance().getProperty("server.context") != null && OrtolangConfig.getInstance().getProperty("server.context").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("server.context"));
			}
			if ( OrtolangConfig.getInstance().getProperty("api.rest.base.path") != null && OrtolangConfig.getInstance().getProperty("api.rest.base.path").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("api.rest.base.path"));
			}
			URI uri;
			if ( OrtolangConfig.getInstance().getProperty("server.port") != null && OrtolangConfig.getInstance().getProperty("server.port").length() > 0 ) {
				uri = new URI("http", null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), path.toString(), null, null);
			} else {
				uri = new URI("http", OrtolangConfig.getInstance().getProperty("server.host"), path.toString(), null);
			}
			return UriBuilder.fromUri(uri);
		} catch (Exception e) {
		}
		return null;
	}
	
	public static UriBuilder getBaseUriBuilder() {
		try {
			StringBuffer path = new StringBuffer();
			if ( OrtolangConfig.getInstance().getProperty("server.context") != null && OrtolangConfig.getInstance().getProperty("server.context").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("server.context"));
			}
			URI uri;
			if ( OrtolangConfig.getInstance().getProperty("server.port") != null && OrtolangConfig.getInstance().getProperty("server.port").length() > 0 ) {
				uri = new URI("http", null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), path.toString(), null, null);
			} else {
				uri = new URI("http", OrtolangConfig.getInstance().getProperty("server.host"), path.toString(), null);
			}
			return UriBuilder.fromUri(uri);
		} catch (Exception e) {
		}
		return null;
	}

}
