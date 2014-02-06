package fr.ortolang.diffusion.browser;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.EntryNotFoundException;
import fr.ortolang.diffusion.registry.RegistryEntry;

public interface BrowserService extends OrtolangService {
	
	public static final String SERVICE_NAME = "Browser";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public RegistryEntry lookup(String key) throws BrowserServiceException, EntryNotFoundException;
	
	public List<RegistryEntry> list(int limit, int offset) throws BrowserServiceException;

}
