package fr.ortolang.diffusion.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
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
import org.jboss.ejb3.annotation.SecurityDomain;

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
import fr.ortolang.diffusion.collaboration.entity.Project;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Remote(CoreService.class)
@Local(CoreServiceLocal.class)
@Stateless(name = CoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class CoreServiceBean implements CoreService, CoreServiceLocal {

	private Logger logger = Logger.getLogger(CoreServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private BinaryStoreService binarystore;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
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

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
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
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating new object for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			String hash = binarystore.put(data);

			DataObject object = new DataObject();
			object.setId(id);
			object.setName(name);
			object.setDescription(description);
			object.setSize(binarystore.size(hash));
			object.setContentType(binarystore.type(hash));
			object.addStream("data-stream", hash);
			em.persist(object);

			registry.register(key, object.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw e;
		} catch (IndexingServiceException | DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException
				| NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			createDataObject(key, name, description, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		createDataObject(key, name, description, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			object.setKey(key);

			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"), "");
			return object;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			throw new CoreServiceException("unable to read object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void readDataObjectContent(String key, OutputStream output) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting content of object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			InputStream input = binarystore.get(object.getStreams().get("data-stream"));
			try {
				IOUtils.copy(input, output);
				object.setNbReads(object.getNbReads() + 1);
				em.merge(object);
				notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read-content"), "");
			} catch (IOException e) {
				throw new CoreServiceException("unable to read data from object with key [" + key + "]", e);
			} finally {
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(output);
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException | MembershipServiceException
				| AuthorisationServiceException e) {
			throw new CoreServiceException("unable to read data from object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void readDataObjectContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			readDataObjectContent(key, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to get data from object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public byte[] readDataObjectContent(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readDataObjectContent(key, baos);
		return baos.toByteArray();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObject(String key, String name, String description) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("object with [" + key + "] is locked and cannot be updated.");
			}

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			object.setName(name);
			object.setDescription(description);
			em.merge(object);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObjectContent(String key, String name, String description, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("object with [" + key + "] is locked, unable to update.");
			}

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
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
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | BinaryStoreServiceException | DataCollisionException | DataNotFoundException
				| MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object content with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObjectContent(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException,
			AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateDataObjectContent(key, name, description, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObjectContent(String key, String name, String description, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		updateDataObjectContent(key, name, description, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "cloning object for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			DataObject clone = new DataObject();
			clone.setId(id);
			clone.setName(object.getName());
			clone.setDescription(object.getDescription());
			clone.setSize(object.getSize());
			clone.setContentType(object.getContentType());
			clone.setStreams(object.getStreams());
			clone.setPreview(object.getPreview());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);

			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "clone"), "key=" + key);
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "forking object for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			DataObject clone = new DataObject();
			clone.setId(id);
			clone.setName(object.getName());
			clone.setDescription(object.getDescription());
			clone.setSize(object.getSize());
			clone.setContentType(object.getContentType());
			clone.setStreams(object.getStreams());
			clone.setPreview(object.getPreview());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "fork"), "key=" + key);
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | MembershipServiceException
				| AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("object with [" + key + "] is locked and cannot be modified.");
			}

			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "delete"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to delete object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCollection(String key, String name, String description) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating new collection for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			Collection collection = new Collection();
			collection.setId(id);
			collection.setName(name);
			collection.setDescription(description);
			em.persist(collection);

			registry.register(key, collection.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException
				| AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during collection creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);

			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			collection.setKey(key);

			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"), "");
			return collection;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			throw new CoreServiceException("unable to read collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateCollection(String key, String name, String description) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("collection with [" + key + "] is locked and cannot be updated.");
			}

			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			collection.setName(name);
			collection.setDescription(description);
			em.merge(collection);

			indexing.reindex(key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void addElementToCollection(String key, String element, boolean inheritSecurity) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "adding element [" + element + "] to collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkOwnership(element, subjects);
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier cidentifier = registry.lookup(key);
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("collection with [" + key + "] is locked and cannot be modified.");
			}

			OrtolangObjectIdentifier eidentifier = registry.lookup(element);
			if (!eidentifier.getService().equals(CoreService.SERVICE_NAME) || eidentifier.getType().equals(Project.OBJECT_TYPE)) {
				throw new CoreServiceException("element [" + element + "] is not an object that can be added to a collection");
			}

			Collection collection = em.find(Collection.class, cidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			if (collection.getElements().contains(element)) {
				throw new CoreServiceException("element [" + element + "] is already in collection with key [" + key + "]");
			}

			collection.addElement(element);
			em.merge(collection);

			if (inheritSecurity) {
				authorisation.copyPolicy(element, key);
			}

			indexing.reindex(key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "add"), "key=" + element);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to add element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeElementFromCollection(String key, String element) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "removing element [" + element + "] from collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(element, subjects, "read");
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("collection with [" + key + "] is locked and cannot be modified.");
			}

			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			if (!collection.getElements().contains(element)) {
				throw new CoreServiceException("element [" + element + "] is NOT in collection with key [" + key + "]");
			}

			collection.removeElement(element);
			em.merge(collection);

			indexing.reindex(key);
			notification
					.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "remove"), "key=" + element);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to remove element [" + element + "] into collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "cloning collection for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Collection.OBJECT_TYPE);

			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			Collection clone = new Collection();
			clone.setId(id);
			clone.setName(collection.getName());
			clone.setDescription(collection.getDescription());
			clone.setElements(collection.getElements());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);

			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "clone"), "key=" + key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "origin="
					+ origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneCollectionContent(String key, String origin) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "cloning collection for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			
			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			
			List<String> cycleDetection = new ArrayList<String> (); 
			cloneCollectionContent(key, origin, cycleDetection);
			
		} catch (RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}
			
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void cloneCollectionContent(String key, String origin, List<String> cycleDetection) throws CoreServiceException, KeyNotFoundException,
			KeyAlreadyExistsException, AccessDeniedException, RegistryServiceException {
		OrtolangObjectIdentifier oidentifier = registry.lookup(origin);
		Collection coll = em.find(Collection.class, oidentifier.getId());
		if (coll == null) {
			throw new CoreServiceException("unable to load collection with id [" + oidentifier.getId() + "] from storage");
		}
		cloneCollection(key, origin);
		cycleDetection.add(origin);
		for (String entry : coll.getElements()) {
			if (cycleDetection.contains(entry)) {
				logger.log(Level.WARNING, "cycle detected during recursive clone");
			} else {
				OrtolangObjectIdentifier eidentifier = registry.lookup(entry);
				String newkey = UUID.randomUUID().toString();
				switch (eidentifier.getType()) {
				case Collection.OBJECT_TYPE:
					cloneCollectionContent(newkey, entry, cycleDetection);
				case DataObject.OBJECT_TYPE:
					cloneDataObject(newkey, entry);
				case MetadataObject.OBJECT_TYPE:
					cloneMetadataObject(newkey, entry);
				case Link.OBJECT_TYPE:
					cloneLink(newkey, entry);
				}
				removeElementFromCollection(key, entry);
				addElementToCollection(key, newkey, false);
			}
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Set<String> listCollectionContent(String key) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "listing collection content for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			
			List<String> cycleDetection = new ArrayList<String> ();
			Set<String> content = new HashSet<String> ();
			listCollectionContent(key, content, cycleDetection);
			
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "list-content"), "");
			return content;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to list collection content for key [" + key + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private void listCollectionContent(String key, Set<String> content, List<String> cycleDetection) throws CoreServiceException, KeyNotFoundException,
			KeyAlreadyExistsException, AccessDeniedException, RegistryServiceException {
		OrtolangObjectIdentifier identifier = registry.lookup(key);
		Collection coll = em.find(Collection.class, identifier.getId());
		if (coll == null) {
			throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
		}
		content.add(key);
		cycleDetection.add(key);
		for (String entry : coll.getElements()) {
			if (cycleDetection.contains(entry)) {
				logger.log(Level.WARNING, "cycle detected during listing content");
			} else {
				OrtolangObjectIdentifier eidentifier = registry.lookup(entry);
				if (eidentifier.getType().equals(Collection.OBJECT_TYPE)) {
					listCollectionContent(entry,content, cycleDetection);
				} else {
					content.add(entry);
				}
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "forking collection for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Collection.OBJECT_TYPE);

			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			Collection clone = new Collection();
			clone.setId(id);
			clone.setName(collection.getName());
			clone.setDescription(collection.getDescription());
			clone.setElements(collection.getElements());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "fork"), "key=" + key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "origin="
					+ origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("collection with [" + key + "] is locked and cannot be modified.");
			}
			registry.delete(key);

			indexing.remove(key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to delete collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createLink(String key, String name, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating new link for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(target);
			if (!identifier.getType().equals(DataObject.OBJECT_TYPE) && !identifier.getType().equals(Collection.OBJECT_TYPE)) {
				throw new CoreServiceException("link can only be a DataObject or a Collection.");
			}

			Link link = new Link();
			link.setId(id);
			link.setName(name);
			link.setTarget(target);
			em.persist(link);

			registry.register(key, link.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException
				| AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during link creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateLink(String key, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating link for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Link.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("link with [" + key + "] is locked and cannot be modified.");
			}

			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}
			link.setName(name);
			em.merge(link);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "update"), "");
		} catch (RegistryServiceException | NotificationServiceException | IndexingServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during link update", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting link for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Link.OBJECT_TYPE);

			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}
			link.setKey(key);

			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "read"), "");
			return link;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			throw new CoreServiceException("unable to get link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneLink(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "cloning link for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Link.OBJECT_TYPE);

			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			Link clone = new Link();
			clone.setId(id);
			clone.setName(link.getName());
			clone.setDynamic(link.isDynamic());
			clone.setTarget(link.getTarget());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);

			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "clone"), "key=" + key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone link with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkLink(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "forking link for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Link.OBJECT_TYPE);

			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}

			String id = UUID.randomUUID().toString();

			Link clone = new Link();
			clone.setId(id);
			clone.setName(link.getName());
			clone.setDynamic(link.isDynamic());
			clone.setTarget(link.getTarget());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "fork"), "key=" + key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork link with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting link for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Link.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("link with [" + key + "] is locked and cannot be modified.");
			}
			registry.delete(key);

			indexing.remove(key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new CoreServiceException("unable to delete link with key [" + key + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private List<Link> findLinksForTarget(String target) throws CoreServiceException {
		TypedQuery<Link> query = em.createNamedQuery("findLinksForTarget", Link.class).setParameter("target", target);
		List<Link> links = query.getResultList();
		return links;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String key, String name, InputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating new metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(target);
			if (!identifier.getType().equals(Link.OBJECT_TYPE) || !identifier.getType().equals(Collection.OBJECT_TYPE) || !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Reference, a DataObject or a Collection.");
			}

			String hash = binarystore.put(data);

			MetadataObject meta = new MetadataObject();
			meta.setId(UUID.randomUUID().toString());
			meta.setName(name);
			meta.setSize(binarystore.size(hash));
			meta.setContentType(binarystore.type(hash));
			meta.setStream(hash);
			// TODO asks to MetadataService whether it recognize the format or ask to the user ??
			// meta.setFormat(format);
			meta.setTarget(target);
			em.persist(meta);

			registry.register(key, meta.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.INFO, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException
				| IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during metadata object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create metadata object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String key, String name, byte[] data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		createMetadataObject(key, name, os, target);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String key, String name, RemoteInputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			createMetadataObject(key, name, os, target);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			meta.setKey(key);

			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "read"), "");
			return meta;
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new CoreServiceException("unable to read metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void readMetadataObjectContent(String key, OutputStream os) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading content from metadata with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			InputStream input = binarystore.get(meta.getStream());
			try {
				IOUtils.copy(input, os);
				notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "read-content"), "");
			} catch (IOException e) {
				throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
			} finally {
				IOUtils.closeQuietly(input);
				IOUtils.closeQuietly(os);
			}
		} catch (DataNotFoundException | BinaryStoreServiceException | RegistryServiceException | NotificationServiceException | MembershipServiceException
				| AuthorisationServiceException e) {
			throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void readMetadataObjectContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		try {
			OutputStream os = RemoteOutputStreamClient.wrap(ros);
			readMetadataObjectContent(key, os);
		} catch (IOException e) {
			throw new CoreServiceException("unable to read content from metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] readMetadataObjectContent(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readMetadataObjectContent(key, baos);
		return baos.toByteArray();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObject(String key, String name, String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("metadata with [" + key + "] is locked and cannot be modified.");
			}

			OrtolangObjectIdentifier tidentifier = registry.lookup(target);
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) || !tidentifier.getType().equals(Collection.OBJECT_TYPE) || !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Reference, a DataObject or a Collection.");
			}

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			meta.setName(name);
			meta.setTarget(target);
			em.merge(meta);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObjectContent(String key, String name, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating metadata content for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("metadata with [" + key + "] is locked and cannot be modified.");
			}

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
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
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "update-content"),
					"");
		} catch (NotificationServiceException | RegistryServiceException | BinaryStoreServiceException | DataCollisionException | DataNotFoundException | IndexingServiceException
				| AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObjectContent(String key, String name, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		updateMetadataObjectContent(key, name, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObjectContent(String key, String name, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateMetadataObjectContent(key, name, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneMetadataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "cloning metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			MetadataObject clone = new MetadataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(meta.getTarget());
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);

			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "clone"), "key="
					+ key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"),
					"origin=" + origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkMetadataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "forking metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			MetadataObject clone = new MetadataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(meta.getTarget());
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.OWNER, caller);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "fork"), "key="
					+ key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"),
					"origin=" + origin);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("metadata with [" + key + "] is locked and cannot be modified.");
			}

			registry.delete(key);

			indexing.remove(key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | MembershipServiceException | AuthorisationServiceException e) {
			throw new CoreServiceException("unable to delete object with key [" + key + "]", e);
		}
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
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
			if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
				return OBJECT_PERMISSIONS_LIST[i][1].split(",");
			}
		}
		throw new OrtolangException("Unable to find object permissions list for object type : " + type);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				return readDataObject(key);
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				return readCollection(key);
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				return readLink(key);
			}

			if (identifier.getType().equals(MetadataObject.OBJECT_TYPE)) {
				return readMetadataObject(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (KeyNotFoundException | CoreServiceException | RegistryServiceException | AccessDeniedException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
		try {
			TypedQuery<DataObject> query = em.createNamedQuery("findObjectByBinaryHash", DataObject.class).setParameter("hash", hash);
			List<DataObject> objects = query.getResultList();
			List<OrtolangObject> oobjects = new ArrayList<OrtolangObject>();
			oobjects.addAll(objects);
			return oobjects;
		} catch (Exception e) {
			throw new OrtolangException("unable to find an object for hash " + hash);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("system")
	public OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableContent content = new OrtolangIndexableContent();

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(object.getName());
				content.addContentPart(object.getDescription());
				content.addContentPart(object.getContentType());
				content.addContentPart(object.getPreview());
				// TODO include the binary content if possible (plain text extraction)
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(collection.getName());
				content.addContentPart(collection.getDescription());
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				Link reference = em.find(Link.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(reference.getName());
			}

			if (identifier.getType().equals(MetadataObject.OBJECT_TYPE)) {
				MetadataObject metadata = em.find(MetadataObject.class, identifier.getId());
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

}
