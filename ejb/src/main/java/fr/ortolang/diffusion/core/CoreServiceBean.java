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
import java.util.Vector;
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

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.core.entity.DigitalCollection;
import fr.ortolang.diffusion.core.entity.DigitalObject;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.BranchNotAllowedException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Remote(CoreService.class)
@Local(CoreServiceLocal.class)
@Stateless(name = CoreService.SERVICE_NAME)
public class CoreServiceBean implements CoreService, CoreServiceLocal {

	private Logger logger = Logger.getLogger(CoreServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private BinaryStoreService binarystore;
	@EJB
	private NotificationService notification;
	@EJB
	private IndexingService indexing;
	
	private static HashMap<String, DigitalObject> objects = new HashMap<String, DigitalObject>(); 
	private static HashMap<String, DigitalCollection> collections = new HashMap<String, DigitalCollection>();
	private static HashMap<String, DigitalReference> references = new HashMap<String, DigitalReference>();

	public CoreServiceBean() {
	}

	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registry = registryService;
	}

	public BinaryStoreService getBinaryStoreService() {
		return binarystore;
	}

	public void setBinaryStoreService(BinaryStoreService binaryStoreService) {
		this.binarystore = binaryStoreService;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notification = notificationService;
	}
	
	public IndexingService getIndexingService() {
		return indexing;
	}

	public void setIndexingService(IndexingService indexing) {
		this.indexing = indexing;
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
	public void createObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new object for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String hash = binarystore.put(data);

			DigitalObject object = new DigitalObject();
			object.setId(id);
			object.setName(name);
			object.setDescription(description);
			object.setSize(binarystore.size(hash));
			object.setContentType(binarystore.type(hash));
			object.addStream("data-stream", hash);
			objects.put(object.getId(), object);

			registry.create(key, object.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, "users:root");
			registry.setProperty(key, OrtolangObjectProperty.OWNER, "users:root");

			indexing.index(key);
			notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			throw e;
		} catch (IndexingServiceException | DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException  e) {
			logger.log(Level.SEVERE, "unexpected error occured during object creation", e);
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	public void createObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyAlreadyExistsException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			createObject(key, name, description, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	public void createObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyAlreadyExistsException {
		InputStream os = new ByteArrayInputStream(data);
		createObject(key, name, description, os);
	}

	@Override
	public DigitalObject getObject(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting object for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			if (objects.containsKey(identifier.getId())) {
				DigitalObject object = objects.get(identifier.getId());
				object.setKey(key);
				notification
						.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"), "");
				return object;
			} else {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get object with key [" + key + "]", e);
		}
	}

	public void getObjectData(String key, OutputStream output) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting data from object with key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			if (objects.containsKey(identifier.getId())) {
				DigitalObject object = objects.get(identifier.getId());
				object.setNbReads(object.getNbReads()+1);
				InputStream input = binarystore.get(object.getStreams().get("data-stream"));
				try {
					IOUtils.copy(input, output);
					notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE,
							OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read-data"), "");
				} catch (IOException e) {
					throw new CoreServiceException("unable to get data from object with key [" + key + "]", e);
				} finally {
					IOUtils.closeQuietly(input);
					IOUtils.closeQuietly(output);
				}
			} else {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to get data from object with key [" + key + "]", e);
		}
	}

	@Override
	public void getObjectData(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			getObjectData(key, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to get data from object with key [" + key + "]", e);
		}
	}

	@Override
	public byte[] getObjectData(String key) throws CoreServiceException, KeyNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		getObjectData(key, baos);
		return baos.toByteArray();
	}

	@Override
	public void updateObject(String key, String name, String description) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			if (objects.containsKey(identifier.getId())) {
				DigitalObject object = objects.get(identifier.getId());
				object.setName(name);
				object.setDescription(description);
				indexing.reindex(key);
				notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "update"),
						"");
			} else {
				throw new CoreServiceException("unable to find object with id [" + identifier.getId() + "] from storage");
			}
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	public void updateObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			if (objects.containsKey(identifier.getId())) {
				String hash = binarystore.put(data);

				DigitalObject object = objects.get(identifier.getId());
				object.setName(name);
				object.setDescription(description);
				object.setSize(binarystore.size(hash));
				object.setContentType(binarystore.type(hash));
				object.removeStream("data-stream");
				object.addStream("data-stream", hash);
				indexing.reindex(key);
				notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "update"),
						"");
			} else {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | BinaryStoreServiceException | DataCollisionException | DataNotFoundException e) {
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	public void updateObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateObject(key, name, description, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	public void updateObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyNotFoundException {
		InputStream os = new ByteArrayInputStream(data);
		updateObject(key, name, description, os);
	}

	@Override
	public void cloneObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "cloning object for origin [" + origin + "] and key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(origin).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);

			if (objects.containsKey(identifier.getId())) {
				DigitalObject object = objects.get(identifier.getId());
				String id = UUID.randomUUID().toString();

				DigitalObject clone = new DigitalObject();
				clone.setId(id);
				clone.setName(object.getName());
				clone.setDescription(object.getDescription());
				clone.setSize(object.getSize());
				clone.setContentType(object.getContentType());
				clone.setStreams(object.getStreams());
				clone.setPreview(object.getPreview());
				objects.put(clone.getId(), clone);

				registry.create(key, clone.getObjectIdentifier(), origin);
				registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
				registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
				registry.setProperty(key, OrtolangObjectProperty.AUTHOR, "users:root");
				registry.setProperty(key, OrtolangObjectProperty.OWNER, "users:root");
				
				List<String> refs = findReferencesForTarget(origin);
				for ( String ref : refs ) {
					updateReference(ref, key);
				}

				indexing.index(key);
				notification.throwEvent(origin, "users:root", DigitalObject.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "clone"), "key=" + key);
				notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"),
						"");
			} else {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException e) {
			throw new CoreServiceException("unable to clone object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	public void deleteObject(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting object for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, "users:root", DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "delete"),
						"");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to delete object with key [" + key + "]", e);
		}
	}

	@Override
	public void createCollection(String key, String name, String description) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new collection for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			DigitalCollection collection = new DigitalCollection();
			collection.setId(id);
			collection.setName(name);
			collection.setDescription(description);
			collections.put(collection.getId(), collection);

			registry.create(key, collection.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, "users:root");
			registry.setProperty(key, OrtolangObjectProperty.OWNER, "users:root");

			notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException e) {
			logger.log(Level.SEVERE, "unexpected error occured during collection creation", e);
			throw new CoreServiceException("unable to create collection with key [" + key + "]", e);
		}
	}

	@Override
	public DigitalCollection getCollection(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting collection for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			if (collections.containsKey(identifier.getId())) {
				DigitalCollection collection = collections.get(identifier.getId());
				collection.setKey(key);
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "read"), "");
				return collection;
			} else {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get collection with key [" + key + "]", e);
		}
	}

	@Override
	public void updateCollection(String key, String name, String description) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating collection for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			if (collections.containsKey(identifier.getId())) {
				DigitalCollection collection = collections.get(identifier.getId());
				collection.setName(name);
				collection.setDescription(description);
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "update"), "");
			} else {
				throw new CoreServiceException("unable to find collection with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to update collection with key [" + key + "]", e);
		}
	}

	@Override
	public void addElementToCollection(String key, String element) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "adding element [" + element + "] to collection for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			if (collections.containsKey(identifier.getId())) {
				OrtolangObjectIdentifier eidentifier = registry.lookup(key).getIdentifier();
				if ( !eidentifier.getService().equals(CoreService.SERVICE_NAME) ) {
					throw new CoreServiceException("element [" + element + "] is not an object that can be added to a collection");
				}
				DigitalCollection collection = collections.get(identifier.getId());
				if ( collection.getElements().contains(element) ) {
					throw new CoreServiceException("element [" + element + "] is already in collection with key [" + key + "]");
				}
				if ( eidentifier.getType().equals(DigitalCollection.OBJECT_TYPE) ) {
					if ( isMember(element, key, new Vector<String> ())) {
						throw new CoreServiceException("unable to add element into collection : cycle detected");
					}
				}
				if ( eidentifier.getType().equals(DigitalReference.OBJECT_TYPE) ) {
					DigitalReference reference = references.get(eidentifier.getId());
					OrtolangObjectIdentifier tidentifier = registry.lookup(reference.getTarget()).getIdentifier();
					if ( tidentifier.getType().equals(DigitalCollection.OBJECT_TYPE) ) {
						if ( isMember(element, key, new Vector<String> ())) {
							throw new CoreServiceException("unable to add element into collection : cycle detected");
						}
					}
				}
				collection.addElement(element);
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "add-element"), "element=" + element);
			} else {
				throw new CoreServiceException("unable to find collection with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to add element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	public void removeElementFromCollection(String key, String element) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "removing element [" + element + "] from collection for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			if (collections.containsKey(identifier.getId())) {
				DigitalCollection collection = collections.get(identifier.getId());
				if ( !collection.getElements().contains(element) ) {
					throw new CoreServiceException("element [" + element + "] is NOT in collection with key [" + key + "]");
				}
				collection.removeElement(element);
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "remove-element"), "element=" + element);
			} else {
				throw new CoreServiceException("unable to find collection with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to remove element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	public void cloneCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "cloning collection for origin [" + origin + "] and key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);

			if (collections.containsKey(identifier.getId())) {
				DigitalCollection collection = collections.get(identifier.getId());
				String id = UUID.randomUUID().toString();

				DigitalCollection clone = new DigitalCollection();
				clone.setId(id);
				clone.setName(collection.getName());
				clone.setDescription(collection.getDescription());
				clone.setElements(collection.getElements());
				collections.put(clone.getId(), clone);

				registry.create(key, clone.getObjectIdentifier(), origin);
				registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
				registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
				registry.setProperty(key, OrtolangObjectProperty.AUTHOR, "users:root");
				registry.setProperty(key, OrtolangObjectProperty.OWNER, "users:root");
				
				List<String> refs = findReferencesForTarget(origin);
				for ( String ref : refs ) {
					updateReference(ref, key);
				}
				
				notification.throwEvent(origin, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "clone"), "key=" + key);
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "create"), "");
			} else {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException e) {
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting collection for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			registry.delete(key);
			if (collections.containsKey(identifier.getId())) {
				collections.remove(identifier.getId());
				notification.throwEvent(key, "users:root", DigitalCollection.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "delete"), "");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to delete collection with key [" + key + "]", e);
		}
	}
	
	@Override
	public void createReference(String key, boolean dynamic, String name, String target) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new reference for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			RegistryEntry entry = registry.lookup(target);
			if ( !entry.getIdentifier().getType().equals(DigitalObject.OBJECT_TYPE) && 
					!entry.getIdentifier().equals(DigitalCollection.OBJECT_TYPE) ){
				throw new CoreServiceException("reference target must be either a DigitalObject nor a DigitalCollection.");
			}
			if ( dynamic && entry.hasChildren() ) {
				throw new CoreServiceException("a dynamic reference target must be the latest version target key and this one has earlier versions");
			}
			
			DigitalReference reference = new DigitalReference();
			reference.setId(id);
			reference.setName(name);
			reference.setDynamic(dynamic);
			reference.setTarget(target);
			references.put(reference.getId(), reference);

			registry.create(key, reference.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, "users:root");
			registry.setProperty(key, OrtolangObjectProperty.OWNER, "users:root");

			notification.throwEvent(key, "users:root", DigitalReference.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException e) {
			logger.log(Level.SEVERE, "unexpected error occured during reference creation", e);
			throw new CoreServiceException("unable to create reference with key [" + key + "]", e);
		}
	}

	@Override
	public DigitalReference getReference(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting reference for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalReference.OBJECT_TYPE);
			if (references.containsKey(identifier.getId())) {
				DigitalReference reference = references.get(identifier.getId());
				reference.setKey(key);
				notification.throwEvent(key, "users:root", DigitalReference.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, "read"), "");
				return reference;
			} else {
				throw new CoreServiceException("unable to load reference with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get reference with key [" + key + "]", e);
		}
	}
	
	public void updateReference(String key, String target) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating reference for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();
			checkObjectType(identifier, DigitalReference.OBJECT_TYPE);
			if (references.containsKey(identifier.getId())) {
				DigitalReference reference = references.get(identifier.getId());
				if ( reference.isDynamic() ) {
					reference.setTarget(target);
					notification.throwEvent(key, "users:root", DigitalReference.OBJECT_TYPE,
							OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, "update"), "target=" + target);
				}
			} else {
				throw new CoreServiceException("unable to load reference with id [" + identifier.getId() + "] from storage");
			}
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get reference with key [" + key + "]", e);
		}
	}
	
	private List<String> findReferencesForTarget(String target) throws CoreServiceException {
		List<String> refs = new ArrayList<String> ();
		for ( DigitalReference reference : references.values() ) {
			if ( reference.getTarget().equals(target) ) {
				try {
					refs.add(registry.lookup(reference.getObjectIdentifier()).getKey());
				} catch ( RegistryServiceException e ) {
					throw new CoreServiceException("unable to find key for reference with identifier : " + reference.getObjectIdentifier(), e);
				} catch (IdentifierNotRegisteredException e) {
				} 
			}
		}
		return refs;
	}
	
	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(DigitalObject.OBJECT_TYPE)) {
				return getObject(key);
			}

			if (identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				return getCollection(key);
			}
			
			if (identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				return getReference(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (KeyNotFoundException | CoreServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
		try {
			List<OrtolangObject> results = new ArrayList<OrtolangObject>();
			for (DigitalObject container : objects.values()) {
				if (container.getStreams().containsKey(hash)) {
					results.add(getObject(container.getKey()));
				}
			}
			return results;
		} catch (KeyNotFoundException | CoreServiceException e) {
			throw new OrtolangException("unable to find an object for hash " + hash);
		}
	}
	
	@Override
	public OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key).getIdentifier();

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}
			
			OrtolangIndexableContent content = new OrtolangIndexableContent();

			if (identifier.getType().equals(DigitalObject.OBJECT_TYPE)) {
				if (!objects.containsKey(identifier.getId())) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				DigitalObject object = objects.get(identifier.getId());
				content.addContentPart(object.getName());
				content.addContentPart(object.getDescription());
				content.addContentPart(object.getContentType());
				content.addContentPart(object.getPreview());
				//TODO include the binary content if possible (plain text extraction)
			}

			if (identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				if (!collections.containsKey(identifier.getId())) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				DigitalCollection collection = collections.get(identifier.getId());
				content.addContentPart(collection.getName());
				content.addContentPart(collection.getDescription());
			}
			
			if (identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				if (!references.containsKey(identifier.getId())) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				DigitalReference reference = references.get(identifier.getId());
				content.addContentPart(reference.getName());
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
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

	private boolean isMember(String collectionKey, String memberKey, Vector<String> cycleDetection) throws CoreServiceException, RegistryServiceException, KeyNotFoundException {
		DigitalCollection collection = getCollection(collectionKey);
		cycleDetection.add(collectionKey);
		if (collection.getElements().contains(memberKey)) {
			return true;
		}
		// TODO maybe prefix entries types in the collection to avoid too much lookup in the registry when checking members types...
		for (String entry : collection.getElements()) {
			OrtolangObjectIdentifier identifier = registry.lookup(entry).getIdentifier();  
			if (identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				if (!cycleDetection.contains(entry) && isMember(entry, memberKey, cycleDetection)) {
					return true;
				}
			}
			if (identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				if (references.containsKey(identifier.getId())) {
					DigitalReference reference = references.get(identifier.getId());
					if (!cycleDetection.contains(reference.getTarget()) && isMember(reference.getTarget(), memberKey, cycleDetection)) {
						return true;
					}
				} else {
					throw new CoreServiceException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
			}
		}
		return false;
	}
	
}
