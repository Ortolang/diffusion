package fr.ortolang.diffusion.registry;

import java.util.List;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectTag;


/**
 * <p>
 * <b>RegistryService</b> for ORTOLANG Diffusion Server.<br/>
 * This service is central to the platform and holds all references to objects managed by diffusion platform.
 * Services are responsible for registering their objects in this registry using the provided API.
 * Services must comply with the DiffusionObjectIdentifier format in order to provide object references that allows
 * the platform to retrieve the good object service and type.  
 * </p>
 * <p>
 * The name used for registry storage concerns the services that are using this registry. The registry is only here to 
 * avoid duplication of registered keys.
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface RegistryService {
	
	public static final String SERVICE_NAME = "registry";
	
	public void create(String key, OrtolangObjectIdentifier identifier) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException;
	
	public void create(String key, OrtolangObjectIdentifier identifier, String parent) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, KeyNotFoundException, BranchNotAllowedException;
	
	public void hide(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void show(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public boolean isHidden(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void delete(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public boolean isDeleted(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void lock(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public boolean isLocked(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public boolean hasChildren(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void tag(String key, String name) throws RegistryServiceException, KeyNotFoundException;
	
	public void untag(String key, String name) throws RegistryServiceException, KeyNotFoundException, TagNotFoundException;
	
	public List<OrtolangObjectTag> taglist() throws RegistryServiceException;
	
	public void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException;
	
	public String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException;
	
	public List<OrtolangObjectProperty> getProperties(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public OrtolangObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String lookup(OrtolangObjectIdentifier identifier) throws RegistryServiceException, IdentifierNotRegisteredException;
	
	public List<String> list(int offset, int limit, String filter) throws RegistryServiceException;
	
	public long count(String filter) throws RegistryServiceException;
	
}
