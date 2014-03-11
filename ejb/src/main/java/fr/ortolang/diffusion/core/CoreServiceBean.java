package fr.ortolang.diffusion.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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
import fr.ortolang.diffusion.core.entity.DigitalMetadata;
import fr.ortolang.diffusion.core.entity.DigitalObject;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.BranchNotAllowedException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
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
	private MembershipService membership;
	@EJB
	private NotificationService notification;
	@EJB
	private IndexingService indexing;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;

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

	public MembershipService getMembershipService() {
		return membership;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}
	
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	public EntityManager getEntityManager() {
		return this.em;
	}

	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
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
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new object for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			String hash = binarystore.put(data);

			DigitalObject object = new DigitalObject();
			object.setId(id);
			object.setName(name);
			object.setDescription(description);
			object.setSize(binarystore.size(hash));
			object.setContentType(binarystore.type(hash));
			object.addStream("data-stream", hash);
			em.persist(object);

			registry.create(key, object.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (IndexingServiceException | DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException
				| NotificationServiceException | IdentifierAlreadyRegisteredException e) {
			logger.log(Level.SEVERE, "unexpected error occured during object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyAlreadyExistsException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			createObject(key, name, description, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyAlreadyExistsException {
		InputStream os = new ByteArrayInputStream(data);
		createObject(key, name, description, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DigitalObject readObject(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			DigitalObject object = em.find(DigitalObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			object.setKey(key);

			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"), "");
			return object;
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to read object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void readObjectContent(String key, OutputStream output) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting content of object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			DigitalObject object = em.find(DigitalObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			InputStream input = binarystore.get(object.getStreams().get("data-stream"));
			try {
				IOUtils.copy(input, output);

				object.setNbReads(object.getNbReads() + 1);
				em.merge(object);

				notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read-data"), "");
			} catch (IOException e) {
				throw new CoreServiceException("unable to read data from object with key [" + key + "]", e);
			} finally {
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to read data from object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void readObjectContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			readObjectContent(key, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to get data from object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public byte[] readObjectContent(String key) throws CoreServiceException, KeyNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readObjectContent(key, baos);
		return baos.toByteArray();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateObject(String key, String name, String description) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			DigitalObject object = em.find(DigitalObject.class, identifier.getId());
			if (object == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			object.setName(name);
			object.setDescription(description);
			em.merge(object);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "update"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateObjectContent(String key, String name, String description, InputStream data) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);
			DigitalObject object = em.find(DigitalObject.class, identifier.getId());
			if (object == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			String hash = binarystore.put(data);

			object.setName(name);
			object.setDescription(description);
			object.setSize(binarystore.size(hash));
			object.setContentType(binarystore.type(hash));
			object.removeStream("data-stream");
			object.addStream("data-stream", hash);
			em.merge(object);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "update"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | BinaryStoreServiceException | DataCollisionException | DataNotFoundException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object content with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateObjectContent(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateObjectContent(key, name, description, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateObjectContent(String key, String name, String description, byte[] data) throws CoreServiceException, KeyNotFoundException {
		InputStream os = new ByteArrayInputStream(data);
		updateObjectContent(key, name, description, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "cloning object for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);

			DigitalObject object = em.find(DigitalObject.class, identifier.getId());
			if (object == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			DigitalObject clone = new DigitalObject();
			clone.setId(id);
			clone.setName(object.getName());
			clone.setDescription(object.getDescription());
			clone.setSize(object.getSize());
			clone.setContentType(object.getContentType());
			clone.setStreams(object.getStreams());
			clone.setPreview(object.getPreview());
			em.persist(clone);

			registry.create(key, clone.getObjectIdentifier(), origin);
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			List<DigitalReference> refs = findReferencesForTarget(origin);
			for (DigitalReference ref : refs) {
				if (ref.isDynamic()) {
					ref.setTarget(key);
					em.merge(ref);
				}
			}

			indexing.index(key);
			notification.throwEvent(origin, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "clone"), "key="
					+ key);
			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteObject(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalObject.OBJECT_TYPE);

			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, DigitalObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "delete"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to delete object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCollection(String key, String name, String description) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new collection for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			DigitalCollection collection = new DigitalCollection();
			collection.setId(id);
			collection.setName(name);
			collection.setDescription(description);
			em.persist(collection);

			registry.create(key, collection.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			indexing.index(key);
			notification
					.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during collection creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DigitalCollection readCollection(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "reading collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);

			DigitalCollection collection = em.find(DigitalCollection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			collection.setKey(key);

			notification.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "read"), "");
			return collection;
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to read collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateCollection(String key, String name, String description) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);

			DigitalCollection collection = em.find(DigitalCollection.class, identifier.getId());
			if (collection == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			collection.setName(name);
			collection.setDescription(description);
			em.merge(collection);

			indexing.reindex(key); 
			notification
					.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	//TODO Refactor this !!
	public void addElementToCollection(String key, String element) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "adding element [" + element + "] to collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier cidentifier = registry.lookup(key);
			checkObjectType(cidentifier, DigitalCollection.OBJECT_TYPE);

			OrtolangObjectIdentifier eidentifier = registry.lookup(element);
			if (!eidentifier.getService().equals(CoreService.SERVICE_NAME)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("element [" + element + "] is not an object that can be added to a collection");
			}

			DigitalCollection collection = em.find(DigitalCollection.class, cidentifier.getId());
			if (collection == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			if (collection.getElements().contains(element)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("element [" + element + "] is already in collection with key [" + key + "]");
			}

			if (eidentifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				if (isMember(element, key, new Vector<String>())) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("unable to add element into collection because collection is already an element of this element so a cycle is detected");
				}
			}
			if (eidentifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				DigitalReference reference = em.find(DigitalReference.class, eidentifier.getId());
				OrtolangObjectIdentifier tidentifier = registry.lookup(reference.getTarget());
				if (tidentifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
					if (isMember(reference.getTarget(), key, new Vector<String>())) {
						ctx.setRollbackOnly();
						throw new CoreServiceException("unable to add element into collection because collection is already an element of this element so a cycle is detected");
					}
				}
			}
			collection.addElement(element);
			em.merge(collection);

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "add-element"), "element=" + element);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to add element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeElementFromCollection(String key, String element) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "removing element [" + element + "] from collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);

			DigitalCollection collection = em.find(DigitalCollection.class, identifier.getId());
			if (collection == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			if (!collection.getElements().contains(element)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("element [" + element + "] is NOT in collection with key [" + key + "]");
			}

			collection.removeElement(element);
			em.merge(collection);

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "remove-element"), "element=" + element);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to remove element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "cloning collection for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);

			DigitalCollection collection = em.find(DigitalCollection.class, identifier.getId());
			if (collection == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			DigitalCollection clone = new DigitalCollection();
			clone.setId(id);
			clone.setName(collection.getName());
			clone.setDescription(collection.getDescription());
			clone.setElements(collection.getElements());
			em.persist(clone);

			registry.create(key, clone.getObjectIdentifier(), origin);
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			List<DigitalReference> refs = findReferencesForTarget(origin);
			for (DigitalReference ref : refs) {
				if (ref.isDynamic()) {
					ref.setTarget(key);
					em.merge(ref);
				}
			}

			indexing.index(key);
			notification.throwEvent(origin, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "clone"),
					"key=" + key);
			notification.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "create"),
					"origin=" + origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalCollection.OBJECT_TYPE);
			registry.delete(key);

			indexing.remove(key);
			notification
					.throwEvent(key, caller, DigitalCollection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to delete collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createReference(String key, boolean dynamic, String name, String target) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new reference for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(target);
			if (!identifier.getType().equals(DigitalObject.OBJECT_TYPE) && !identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("reference target must be either a DigitalObject nor a DigitalCollection.");
			}
			if (dynamic && registry.hasChildren(target)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("a dynamic reference target must be the latest version target key and this one has earlier versions");
			}

			DigitalReference reference = new DigitalReference();
			reference.setId(id);
			reference.setName(name);
			reference.setDynamic(dynamic);
			reference.setTarget(target);
			em.persist(reference);

			registry.create(key, reference.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, DigitalReference.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during reference creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create reference with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DigitalReference readReference(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting reference for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalReference.OBJECT_TYPE);

			DigitalReference reference = em.find(DigitalReference.class, identifier.getId());
			if (reference == null) {
				throw new CoreServiceException("unable to load reference with id [" + identifier.getId() + "] from storage");
			}
			reference.setKey(key);

			notification.throwEvent(key, caller, DigitalReference.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, "read"), "");
			return reference;
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to get reference with key [" + key + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private List<DigitalReference> findReferencesForTarget(String target) throws CoreServiceException {
		TypedQuery<DigitalReference> query = em.createNamedQuery("findReferencesForTarget", DigitalReference.class).setParameter("target", target);
		List<DigitalReference> refs = query.getResultList();
		return refs;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadata(String key, String name, InputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating new metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(target);
			if (!identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("metadata target must be a DigitalReference.");
			}

			String hash = binarystore.put(data);

			DigitalMetadata meta = new DigitalMetadata();
			meta.setId(UUID.randomUUID().toString());
			meta.setName(name);
			meta.setSize(binarystore.size(hash));
			meta.setContentType(binarystore.type(hash));
			meta.setStream(hash);
			// TODO asks to MetadataService whether it recognize the format or ask to the user ??
			// meta.setFormat(format);
			meta.setTarget(target);
			em.persist(meta);

			registry.create(key, meta.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException
				| IdentifierAlreadyRegisteredException | IndexingServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during metadata object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create metadata object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadata(String key, String name, byte[] data, String target) throws CoreServiceException, KeyAlreadyExistsException {
		InputStream os = new ByteArrayInputStream(data);
		createMetadata(key, name, os, target);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadata(String key, String name, RemoteInputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			createMetadata(key, name, os, target);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DigitalMetadata readMetadata(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "reading metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);

			DigitalMetadata meta = em.find(DigitalMetadata.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			meta.setKey(key);

			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"), "");
			return meta;
		} catch (NotificationServiceException | RegistryServiceException e) {
			throw new CoreServiceException("unable to read metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void readMetadataContent(String key, OutputStream os) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "reading content from metadata with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);

			DigitalMetadata meta = em.find(DigitalMetadata.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			InputStream input = binarystore.get(meta.getStream());
			try {
				IOUtils.copy(input, os);
				notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read-data"),
						"");
			} catch (IOException e) {
				throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
			} finally {
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(os);
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void readMetadataContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			readMetadataContent(key, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] readMetadataContent(String key) throws CoreServiceException, KeyNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readMetadataContent(key, baos);
		return baos.toByteArray();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadata(String key, String name) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);

			DigitalMetadata meta = em.find(DigitalMetadata.class, identifier.getId());
			if (meta == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			meta.setName(name);
			em.merge(meta);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataContent(String key, String name, InputStream data) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "updating metadata content for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);

			DigitalMetadata meta = em.find(DigitalMetadata.class, identifier.getId());
			if (meta == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			String hash = binarystore.put(data);

			meta.setName(name);
			meta.setSize(binarystore.size(hash));
			meta.setContentType(binarystore.type(hash));
			meta.setStream(hash);
			em.merge(meta);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "update-content"), "");
		} catch (NotificationServiceException | RegistryServiceException | BinaryStoreServiceException | DataCollisionException | DataNotFoundException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataContent(String key, String name, byte[] data) throws CoreServiceException, KeyNotFoundException {
		InputStream os = new ByteArrayInputStream(data);
		updateMetadataContent(key, name, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataContent(String key, String name, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateMetadataContent(key, name, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneMetadata(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "cloning metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);

			DigitalMetadata meta = em.find(DigitalMetadata.class, identifier.getId());
			if (meta == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			DigitalMetadata clone = new DigitalMetadata();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(meta.getTarget());
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.create(key, clone.getObjectIdentifier(), origin);
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "clone"),
					"key=" + key);
			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "create"),
					"origin=" + origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteMetadata(String key) throws CoreServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DigitalMetadata.OBJECT_TYPE);
			registry.delete(key);

			indexing.remove(key);
			notification.throwEvent(key, caller, DigitalMetadata.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "delete"),
					"");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
			throw new CoreServiceException("unable to delete object with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(DigitalObject.OBJECT_TYPE)) {
				return readObject(key);
			}

			if (identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				return readCollection(key);
			}

			if (identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				return readReference(key);
			}

			if (identifier.getType().equals(DigitalMetadata.OBJECT_TYPE)) {
				return readMetadata(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (KeyNotFoundException | CoreServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
		try {
			TypedQuery<DigitalObject> query = em.createNamedQuery("findObjectByBinaryHash", DigitalObject.class).setParameter("hash", hash);
			List<DigitalObject> objects = query.getResultList();
			List<OrtolangObject> oobjects = new ArrayList<OrtolangObject> ();
			oobjects.addAll(objects);
			return oobjects;
		} catch ( Exception e) {
			throw new OrtolangException("unable to find an object for hash " + hash);
		}	
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableContent content = new OrtolangIndexableContent();

			if (identifier.getType().equals(DigitalObject.OBJECT_TYPE)) {
				DigitalObject object = em.find(DigitalObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(object.getName());
				content.addContentPart(object.getDescription());
				content.addContentPart(object.getContentType());
				content.addContentPart(object.getPreview());
				// TODO include the binary content if possible (plain text extraction)
			}

			if (identifier.getType().equals(DigitalCollection.OBJECT_TYPE)) {
				DigitalCollection collection = em.find(DigitalCollection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(collection.getName());
				content.addContentPart(collection.getDescription());
			}

			if (identifier.getType().equals(DigitalReference.OBJECT_TYPE)) {
				DigitalReference reference = em.find(DigitalReference.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(reference.getName());
			}
			
			if (identifier.getType().equals(DigitalMetadata.OBJECT_TYPE)) {
				DigitalMetadata metadata = em.find(DigitalMetadata.class, identifier.getId());
				if (metadata == null) {
					throw new OrtolangException("unable to load metadata with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(metadata.getName());
				content.addContentPart(metadata.getContentType());
				content.addContentPart(metadata.getFormat());
				content.addContentPart(metadata.getTarget());
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

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private boolean isMember(String collection, String member, Vector<String> cycleDetection) throws CoreServiceException, RegistryServiceException, KeyNotFoundException {
		OrtolangObjectIdentifier cidentifier = registry.lookup(collection);
		DigitalCollection coll = em.find(DigitalCollection.class, cidentifier.getId());
		if (coll == null) {
			throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
		}
		cycleDetection.add(collection);
		if (coll.getElements().contains(member)) {
			return true;
		}
		for (String entry : coll.getElements()) {
			OrtolangObjectIdentifier eidentifier = registry.lookup(entry);
			DigitalReference reference = em.find(DigitalReference.class, eidentifier.getId());
			if (reference == null) {
				throw new CoreServiceException("unable to load reference with id [" + eidentifier.getId() + "] from storage");
			}
			if (!cycleDetection.contains(reference.getTarget()) && isMember(reference.getTarget(), member, cycleDetection)) {
				return true;
			}
		}
		return false;
	}

}
