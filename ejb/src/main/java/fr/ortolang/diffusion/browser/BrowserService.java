package fr.ortolang.diffusion.browser;

import java.util.List;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectTag;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.TagNotFoundException;

public interface BrowserService extends OrtolangService {
	
	public static final String SERVICE_NAME = "browser";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public OrtolangObjectIdentifier lookup(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public List<String> list(int limit, int offset, String service, String type) throws BrowserServiceException;
	
	public long count(String service, String type) throws BrowserServiceException;
	
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public List<OrtolangObjectVersion> history(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public OrtolangObjectVersion getVersion(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException;
	
	public void setProperty(String key, String name, String value) throws BrowserServiceException, KeyNotFoundException;
	
	public List<OrtolangObjectTag> listAllTags() throws BrowserServiceException, KeyNotFoundException;
	
	public List<OrtolangObjectTag> listTags(String key) throws BrowserServiceException, KeyNotFoundException;
	
	public void addTag(String key, String name) throws BrowserServiceException, KeyNotFoundException;
	
	public OrtolangObjectTag getTag(String name) throws BrowserServiceException, TagNotFoundException;
	
	public void removeTag(String key, String name) throws BrowserServiceException, KeyNotFoundException, TagNotFoundException;
	
}
