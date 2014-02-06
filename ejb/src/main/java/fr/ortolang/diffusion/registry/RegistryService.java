package fr.ortolang.diffusion.registry;

import java.util.List;


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
	
	public static final String SERVICE_NAME = "Registry";
	
	public void create(RegistryEntry entry) throws RegistryServiceException, EntryAlreadyExistsException;
	
	public void update(RegistryEntry entry) throws RegistryServiceException, EntryNotFoundException;
	
	public void delete(String key) throws RegistryServiceException, EntryNotFoundException;
	
	public RegistryEntry lookup(String key) throws RegistryServiceException, EntryNotFoundException;
	
	public List<RegistryEntry> list(int offset, int limit) throws RegistryServiceException;
	
	public long count() throws RegistryServiceException;
	

}
