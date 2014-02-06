package fr.ortolang.diffusion.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.io.IOUtils;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteOutputStream;
import com.healthmarketscience.rmiio.RemoteOutputStreamClient;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangNamingConvention;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.entity.ObjectContainer;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.EntryAlreadyExistsException;
import fr.ortolang.diffusion.registry.EntryNotFoundException;
import fr.ortolang.diffusion.registry.RegistryEntry;
import fr.ortolang.diffusion.registry.RegistryEntryState;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Remote(CoreService.class)
@Local(CoreServiceLocal.class)
@Stateless(name=CoreService.SERVICE_NAME)
public class CoreServiceBean implements CoreService, CoreServiceLocal {
	
	private Logger logger = Logger.getLogger(CoreServiceBean.class.getName());
	
	@EJB
	private RegistryService registryService;
	@EJB
	private BinaryStoreService binaryStoreService;
	@EJB
	private NotificationService notificationService;
	private HashMap<String, ObjectContainer> containers;
	
	public CoreServiceBean() throws CoreServiceException {
		containers = new HashMap<String, ObjectContainer>();
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
	
	public NotificationService getNotificationService() {
		return notificationService;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notificationService = notificationService;
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
	public void createContainer(String key, String name) throws CoreServiceException, EntryAlreadyExistsException {
		logger.log(Level.INFO, "creating new container for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			ObjectContainer container = new ObjectContainer();
			container.setId(id);
			container.setName(name);
			containers.put(container.getId(), container);
			registryService.create(new RegistryEntry(key, RegistryEntryState.USED, container.getObjectIdentifier()));
			notificationService.throwEvent(container.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "create"), "");
		} catch ( EntryAlreadyExistsException e ) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			//TODO rollback
			throw e;
		} catch (RegistryServiceException e) {
			logger.log(Level.SEVERE, "unable to register object",e);
			//TODO rollback
			throw new CoreServiceException("unable to create container with key [" + key + "]", e);
		} catch (NotificationServiceException e) {
			logger.log(Level.SEVERE, "error during creation notification",e);
			//TODO rollback
			throw new CoreServiceException("unable to create container with key [" + key + "]", e);
		}
	}

	@Override
	public ObjectContainer getContainer(String key) throws CoreServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "getting container for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				notificationService.throwEvent(identifier.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "read"), "");
				return containers.get(identifier.getId());
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get container with key [" + key + "]", e);
		}
	}

	@Override
	public void deleteContainer(String key) throws CoreServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "deleting container for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			registryService.delete(key);
			if ( containers.containsKey(identifier.getId()) ) {
				containers.remove(identifier.getId());
				notificationService.throwEvent(identifier.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "delete"), "");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to delete container with key [" + key + "]", e);
		}
	}
	
	@Override
	public void addDataStreamToContainer(String key, String name, InputStream data) throws CoreServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "adding stream to container with key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				String hash = binaryStoreService.put(data);
				container.addStream(name, hash);
				notificationService.throwEvent(identifier.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "add-stream"), "");
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (DataCollisionException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to add stream to container with key [" + key + "]", e);
		} 
	}

	@Override
	public void addDataStreamToContainer(String key, String name, RemoteInputStream data) throws CoreServiceException, EntryNotFoundException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			addDataStreamToContainer(key, name, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to add stream to container with key [" + key + "]", e);
		}
	}
	
	@Override
	public void addDataStreamToContainer(String key, String name, byte[] data) throws CoreServiceException, EntryNotFoundException {
		InputStream os = new ByteArrayInputStream(data);
		addDataStreamToContainer(key, name, os);
	}

	@Override
	public void removeDataStreamFromContainer(String key, String name) throws CoreServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "removing stream from container with key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				container.removeStream(name);
				notificationService.throwEvent(identifier.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "remove-stream"), "");
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to remove stream from container with key [" + key + "]", e);
		} 
	}

	@Override
	public void getDataStreamFromContainer(String key, String name, OutputStream output) throws CoreServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "getting stream from container with key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
			checkObjectType(identifier, ObjectContainer.OBJECT_TYPE);
			if ( containers.containsKey(identifier.getId()) ) {
				ObjectContainer container = containers.get(identifier.getId());
				if ( container.getStreams().containsKey(name) ) {
					InputStream input = binaryStoreService.get(container.getStreams().get(name));
					try {
						IOUtils.copy(input, output);
						notificationService.throwEvent(identifier.getId(), "connectedUser", ObjectContainer.OBJECT_TYPE, OrtolangNamingConvention.buildEventType(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, "get-stream"), "");
					} catch ( IOException e ) {
						throw new CoreServiceException("unable to get stream from container with key [" + key + "]", e);
					} finally {
						IOUtils.closeQuietly(input);
						IOUtils.closeQuietly(output);
					}
				} else {
					throw new CoreServiceException("no stream with name [" + name + "] has been found for container with key [" + key + "]");
				}
			} else {
				throw new CoreServiceException("unable to load container with id [" + identifier.getId() + "] from storage");
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to get stream from container with key [" + key + "]", e);
		}
	}
	
	@Override
	public void getDataStreamFromContainer(String key, String name, RemoteOutputStream ros) throws CoreServiceException, EntryNotFoundException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			getDataStreamFromContainer(key, name, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to get stream from container with key [" + key + "]", e);
		}
	}
	
	@Override
	public byte[] getDataStreamFromContainer(String key, String name) throws CoreServiceException, EntryNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		getDataStreamFromContainer(key, name, baos);
		return baos.toByteArray();
	}
	
	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registryService.lookup(key).getIdentifier();
	
			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}
	
			if (identifier.getType().equals(ObjectContainer.OBJECT_TYPE)) {
				return getContainer(key);
			}
			
			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch ( EntryNotFoundException | CoreServiceException | RegistryServiceException e ) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
		try {
			List<OrtolangObject> results = new ArrayList<OrtolangObject>();
			for ( ObjectContainer container : containers.values() ) {
				if ( container.getStreams().containsKey(hash) ) {
					results.add(getContainer(container.getKey()));
				}
			}
			return results;
		} catch ( EntryNotFoundException | CoreServiceException e ) {
			throw new OrtolangException("unable to find an object for hash " + hash);
		}
	}
	
	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws CoreServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new CoreServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new CoreServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}
