package fr.ortolang.diffusion.store.triple;

import java.net.URI;
import java.net.URISyntaxException;

import fr.ortolang.diffusion.OrtolangConfig;

public class URIHelper {
	
	public static String fromKey(String key) throws TripleStoreServiceException {
		try {
			if ( key == null )  {
				return null;
			} else {
				StringBuffer path = new StringBuffer();
				if ( OrtolangConfig.getInstance().getProperty("server.context") != null && OrtolangConfig.getInstance().getProperty("server.context").length() > 0 ) {
					path.append(OrtolangConfig.getInstance().getProperty("server.context"));
				}
				if ( OrtolangConfig.getInstance().getProperty("api.rest.objects.path") != null && OrtolangConfig.getInstance().getProperty("api.rest.objects.path").length() > 0 ) {
					path.append(OrtolangConfig.getInstance().getProperty("api.rest.objects.path"));
				}
				URI uri = new URI("http", null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), path.append("/").append(key).toString(), null, null);
				return uri.toString();
			}
		} catch ( URISyntaxException use ) {
			throw new TripleStoreServiceException(use);
		}
	}
	
	public static String toKey(String uri) throws TripleStoreServiceException {
		try {
			if ( uri == null )  {
				return null;
			} else {
				URI puri = new URI(uri);
				return puri.getPath();
			}
		} catch ( URISyntaxException use ) {
			throw new TripleStoreServiceException(use);
		}
	}
	
}