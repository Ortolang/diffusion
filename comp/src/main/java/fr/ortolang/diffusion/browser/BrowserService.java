package fr.ortolang.diffusion.browser;

import java.util.List;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface BrowserService extends OrtolangService {
	
	public static final String SERVICE_NAME = "browser";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { };
	
	public List<String> list(int limit, int offset, String service, String type, OrtolangObjectState.Status status, boolean itemsOnly) throws BrowserServiceException;
	
	public long count(String service, String type, OrtolangObjectState.Status status, boolean itemOnly) throws BrowserServiceException;
	
	public OrtolangObjectIdentifier lookup(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException, AccessDeniedException;
	
	public void setProperty(String key, String name, String value) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectInfos getInfos(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectVersion getVersion(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	 
	public List<OrtolangObjectVersion> getHistory(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
}
