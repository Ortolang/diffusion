package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.DiffusionObject;
import fr.ortolang.diffusion.DiffusionObjectIdentifier;
import fr.ortolang.diffusion.DiffusionServiceException;
import fr.ortolang.diffusion.core.entity.ObjectContainer;
import fr.ortolang.diffusion.registry.KeyAlreadyBoundException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;

public class InMemoryCoreService implements CoreService {
	
	private static InMemoryCoreService instance;
	private Logger logger = Logger.getLogger(InMemoryCoreService.class.getName());
	private RegistryService registryService;
	private BinaryStoreService binaryStoreService;
	private HashMap<String, ObjectContainer> containers;
	
	private InMemoryCoreService() throws CoreServiceException {
		containers = new HashMap<String, ObjectContainer>();
	}
	
	public static InMemoryCoreService getInstance() throws CoreServiceException {
		if ( instance == null ) {
			instance = new InMemoryCoreService();
		}
		return instance;
	}
	
	public RegistryService getRegistryService() {
		return registryService;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registryService = registryService;
	}

	public BinaryStoreService getBinaryStoreService() {
		return binaryStoreService;
	}

	public void setBinaryStoreService(BinaryStoreService binaryStoreService) {
		this.binaryStoreService = binaryStoreService;
	}

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return OBJECT_TYPE_LIST;
	}
	
	@Override
	public void createContainer(String key, String name) throws CoreServiceException, KeyAlreadyBoundException {
		logger.log(Level.INFO, "creating new container for key [" + key + "]");
		ObjectContainer container = new ObjectContainer();
		container.setId(UUID.randomUUID().toString());
		container.setName(name);
		containers.put(container.getId(), container);
		try {
			registryService.bind(key, container.getDiffusionObjectIdentifier());
		} catch ( KeyAlreadyBoundException e ) {
			logger.log(Level.INFO, "the key [" + key + "] is already binded to another object");
			containers.remove(container.getId());
			throw e;
		} catch (RegistryServiceException e) {
			logger.log(Level.SEVERE, "unable to bind object",e);
			containers.remove(container.getId());
			throw new CoreServiceException("unable to create container with key [" + key + "]", e);
		}
	}

	@Override
	public ObjectContainer getContainer(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting container for key [" + key + "]");
		try {
			DiffusionObjectIdentifier identifier = registryService.lookup(key);
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				return containers.get(identifier.getId());
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (RegistryServiceException e) {
			throw new CoreServiceException("unable to get container with key [" + key + "]", e);
		}
	}

	@Override
	public void deleteContainer(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting container for key [" + key + "]");
		try {
			DiffusionObjectIdentifier identifier = registryService.lookup(key);
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			registryService.unbind(key);
			if ( containers.containsKey(identifier.getId()) ) {
				containers.remove(identifier.getId());
			}
		} catch (RegistryServiceException e) {
			throw new CoreServiceException("unable to delete container with key [" + key + "]", e);
		}
	}

	@Override
	public void addDataStreamToContainer(String key, String name, InputStream data) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "adding stream to container with key [" + key + "]");
		try {
			DiffusionObjectIdentifier identifier = registryService.lookup(key);
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				String hash = binaryStoreService.put(data);
				container.addStream(name, hash);
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (DataCollisionException | BinaryStoreServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to add stream to container with key [" + key + "]", e);
		} 
	}

	@Override
	public void removeDataStreamFromContainer(String key, String name) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "removing stream from container with key [" + key + "]");
		try {
			DiffusionObjectIdentifier identifier = registryService.lookup(key);
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				container.removeStream(name);
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (RegistryServiceException e) {
			throw new CoreServiceException("unable to remove stream from container with key [" + key + "]", e);
		} 
	}

	@Override
	public InputStream getDataStreamFromContainer(String key, String name) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting stream from container with key [" + key + "]");
		try {
			DiffusionObjectIdentifier identifier = registryService.lookup(key);
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				if ( container.getStreams().containsKey(name) ) {
					return binaryStoreService.get(container.getStreams().get(name));
				} else {
					throw new CoreServiceException("no stream with name [" + name + "] has been found for container with key [" + key + "]");
				}
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get stream from container with key [" + key + "]", e);
		} 
	}
	
	@Override
	public DiffusionObject findObject(DiffusionObjectIdentifier identifier) throws DiffusionServiceException {
		if ( containers.containsKey(identifier.getId()) ) {
			return containers.get(identifier.getId());
		} 
		return null;
	}

	@Override
	public List<DiffusionObject> findObjectByBinaryHash(String hash) throws DiffusionServiceException {
		List<DiffusionObject> results = new ArrayList<DiffusionObject>();
		for ( ObjectContainer container : containers.values() ) {
			if ( container.getStreams().containsKey(hash) ) {
				results.add(container);
			}
		}
		return results;
	}
	
	private void checkObjectType(DiffusionObjectIdentifier identifier, String objectType) throws CoreServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new CoreServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new CoreServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}
