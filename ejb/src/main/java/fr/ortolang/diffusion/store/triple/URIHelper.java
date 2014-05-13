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
				URI uri = new URI("http", null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), "/" + key, null, null);
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