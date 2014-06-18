package fr.ortolang.diffusion.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.ejb3.annotation.SecurityDomain;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteOutputStream;
import com.healthmarketscience.rmiio.RemoteOutputStreamClient;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
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
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
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
import fr.ortolang.diffusion.store.triple.Triple;
import fr.ortolang.diffusion.store.triple.TripleHelper;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;
import fr.ortolang.diffusion.store.triple.URIHelper;

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
		logger.log(Level.FINE, "creating new object for key [" + key + "]");
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
		logger.log(Level.FINE, "getting object for key [" + key + "]");
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
		logger.log(Level.FINE, "getting content of object with key [" + key + "]");
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
		logger.log(Level.FINE, "updating object for key [" + key + "]");
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
	public void updateDataObjectContent(String key, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating object for key [" + key + "]");
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
	public void updateDataObjectContent(String key, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateDataObjectContent(key, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObjectContent(String key, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		updateDataObjectContent(key, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "cloning object for origin [" + origin + "] and key [" + key + "]");
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
			Map<String, String> streams = new HashMap<String, String>();
			streams.putAll(object.getStreams());
			clone.setStreams(streams);
			clone.setPreview(object.getPreview());
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : object.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				systemCloneMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			
			authorisation.clonePolicy(key, origin);
			
			indexing.index(key);
			notification.throwEvent(origin, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "clone"), "key=" + key);
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "forking object for origin [" + origin + "] and key [" + key + "]");
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
			Map<String, String> streams = new HashMap<String, String>();
			streams.putAll(object.getStreams());
			clone.setPreview(object.getPreview());
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : object.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				forkMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "fork"), "key=" + key);
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | MembershipServiceException
				| AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork object with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting object for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("object with [" + key + "] is locked and cannot be modified.");
			}

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			for (String metadata : object.getMetadatas()) {
				registry.delete(metadata);
				indexing.remove(metadata);
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
		logger.log(Level.FINE, "creating new collection for key [" + key + "]");
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

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.FINE, "the key [" + key + "] is already used");
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
		logger.log(Level.FINE, "reading collection for key [" + key + "]");
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
		logger.log(Level.FINE, "updating collection for key [" + key + "]");
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
		logger.log(Level.FINE, "adding element [" + element + "] to collection for key [" + key + "]");
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
			if (!eidentifier.getType().equals(Collection.OBJECT_TYPE) && !eidentifier.getType().equals(DataObject.OBJECT_TYPE) && !eidentifier.getType().equals(Link.OBJECT_TYPE)) {
				throw new CoreServiceException("element [" + element + "] is not an object that can be added to a collection");
			}

			Collection collection = em.find(Collection.class, cidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			if (collection.addElement(element)) {
				em.merge(collection);
			} else {
				throw new CoreServiceException("element [" + element + "] is already in collection with key [" + key + "]");
			}

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
		logger.log(Level.FINE, "removing element [" + element + "] from collection for key [" + key + "]");
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

			if (collection.removeElement(element)) {
				em.merge(collection);
			} else {
				throw new CoreServiceException("element [" + element + "] is NOT in collection with key [" + key + "]");
			}

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
		logger.log(Level.FINE, "cloning collection for origin [" + origin + "] and key [" + key + "]");
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
			Set<String> elements = new HashSet<String>();
			elements.addAll(collection.getElements());
			clone.setElements(elements);
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : collection.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				systemCloneMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			
			authorisation.clonePolicy(key, origin);
			
			indexing.index(key);
			notification.throwEvent(origin, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "clone"), "key=" + key);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneCollectionContent(String key, String origin) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "cloning collection for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, Collection.OBJECT_TYPE);

			List<String> cycleDetection = new ArrayList<String>();
			cloneCollectionContent(key, origin, cycleDetection);

		} catch (RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone collection with origin [" + origin + "] and key [" + key + "]", e);
		}

	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void cloneCollectionContent(String key, String origin, List<String> cycleDetection) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException,
			AccessDeniedException, RegistryServiceException {
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
					break;
				case DataObject.OBJECT_TYPE:
					cloneDataObject(newkey, entry);
					break;
				case Link.OBJECT_TYPE:
					cloneLink(newkey, entry);
					break;
				}
				removeElementFromCollection(key, entry);
				addElementToCollection(key, newkey, false);
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Set<String> listCollectionContent(String key) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "listing collection content for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);

			List<String> cycleDetection = new ArrayList<String>();
			Set<String> content = new HashSet<String>();
			listCollectionContent(key, content, cycleDetection);

			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "list-content"), "");
			return content;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to list collection content for key [" + key + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private void listCollectionContent(String key, Set<String> content, List<String> cycleDetection) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException,
			AccessDeniedException, RegistryServiceException {
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
					listCollectionContent(entry, content, cycleDetection);
				} else {
					content.add(entry);
				}
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "forking collection for origin [" + origin + "] and key [" + key + "]");
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
			Set<String> elements = new HashSet<String>();
			elements.addAll(collection.getElements());
			clone.setElements(collection.getElements());
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : collection.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				forkMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "fork"), "key=" + key);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork collection with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting collection for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Collection.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("collection with [" + key + "] is locked and cannot be modified.");
			}
			
			Collection collection = em.find(Collection.class, identifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
			}
			for (String metadata : collection.getMetadatas()) {
				registry.delete(metadata);
				indexing.remove(metadata);
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
		logger.log(Level.FINE, "creating new link for key [" + key + "]");
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

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.FINE, "the key [" + key + "] is already used");
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
		logger.log(Level.FINE, "updating link for key [" + key + "]");
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
		logger.log(Level.FINE, "getting link for key [" + key + "]");
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
		logger.log(Level.FINE, "cloning link for origin [" + origin + "] and key [" + key + "]");
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
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : link.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				systemCloneMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			
			authorisation.clonePolicy(key, origin);
			
			indexing.index(key);
			notification.throwEvent(origin, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "clone"), "key=" + key);
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone link with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkLink(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "forking link for origin [" + origin + "] and key [" + key + "]");
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
			Set<String> metadatas = new HashSet<String>();
			for (String metadata : link.getMetadatas()) {
				String mid = UUID.randomUUID().toString();
				forkMetadataObject(mid, metadata, key);
				metadatas.add(mid);
			}
			clone.setMetadatas(metadatas);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "fork"), "key=" + key);
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork link with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting link for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Link.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("link with [" + key + "] is locked and cannot be modified.");
			}
			
			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}
			for (String metadata : link.getMetadatas()) {
				registry.delete(metadata);
				indexing.remove(metadata);
			}
			
			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new CoreServiceException("unable to delete link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding metadata for target [" + target + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(target, subjects, "read");

			TypedQuery<Link> query = em.createNamedQuery("findLinksForTarget", Link.class).setParameter("target", target);
			List<Link> links = query.getResultList();
			List<String> results = new ArrayList<String>();
			for (Link link : links) {
				String key = registry.lookup(link.getObjectIdentifier());
				results.add(key);
			}
			notification.throwEvent("", caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "find"), "target=" + target);
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException
				| IdentifierNotRegisteredException e) {
			throw new CoreServiceException("unable to find link for target [" + target + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String key, String name, InputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating new metadata with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(target, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(target);
			if (!identifier.getType().equals(Link.OBJECT_TYPE) && !identifier.getType().equals(Collection.OBJECT_TYPE) && !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			String hash = binarystore.put(data);

			MetadataObject meta = new MetadataObject();
			meta.setId(UUID.randomUUID().toString());
			meta.setName(name);
			meta.setSize(binarystore.size(hash));
			meta.setContentType(binarystore.type(hash));
			meta.setStream(hash);
			meta.setTarget(target);
			em.persist(meta);

			switch (identifier.getType()) {
			case Collection.OBJECT_TYPE:
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				collection.addMetadata(key);
				em.merge(collection);
				break;
			case DataObject.OBJECT_TYPE:
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				object.addMetadata(key);
				em.merge(object);
				break;
			case Link.OBJECT_TYPE:
				Link link = em.find(Link.class, identifier.getId());
				if (link == null) {
					throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
				}
				link.addMetadata(key);
				em.merge(link);
				break;
			}

			registry.register(key, meta.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			indexing.reindex(target);

			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"), "");
			notification
					.throwEvent(target, caller, identifier.getType(), OrtolangEvent.buildEventType(identifier.getService(), identifier.getType(), "add-metadata"), "key=" + key);
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.FINE, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (DataCollisionException | DataNotFoundException | BinaryStoreServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException
				| IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException | MembershipServiceException e) {
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
		logger.log(Level.FINE, "reading metadata for key [" + key + "]");
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
		logger.log(Level.FINE, "reading content from metadata with key [" + key + "]");
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
	public void updateMetadataObject(String key, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating metadata for key [" + key + "]");
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
			meta.setName(name);
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
	public void updateMetadataObjectContent(String key, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating metadata content for key [" + key + "]");
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
	public void updateMetadataObjectContent(String key, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		InputStream os = new ByteArrayInputStream(data);
		updateMetadataObjectContent(key, os);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObjectContent(String key, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		try {
			InputStream os = RemoteInputStreamClient.wrap(data);
			updateMetadataObjectContent(key, os);
		} catch (IOException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to update metadata with key [" + key + "]", e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void systemCloneMetadataObject(String key, String origin, String target) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "system clone of metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			MetadataObject clone = new MetadataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(target);
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "clone"), "key="
					+ key);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void cloneMetadataObject(String key, String origin, String target) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "cloning metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(target, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			OrtolangObjectIdentifier tidentifier = registry.lookup(target);
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) || !tidentifier.getType().equals(Collection.OBJECT_TYPE) || !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			MetadataObject clone = new MetadataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(target);
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);

			authorisation.clonePolicy(key, origin);

			indexing.index(key);
			notification.throwEvent(origin, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "clone"), "key="
					+ key);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to clone metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void forkMetadataObject(String key, String origin, String target) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "forking metadata for origin [" + origin + "] and key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			authorisation.checkPermission(origin, subjects, "read");
			authorisation.checkPermission(target, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(origin);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

			OrtolangObjectIdentifier tidentifier = registry.lookup(target);
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) || !tidentifier.getType().equals(Collection.OBJECT_TYPE) || !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			MetadataObject clone = new MetadataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(meta.getName());
			clone.setTarget(target);
			clone.setSize(meta.getSize());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(origin, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "fork"), "key="
					+ key);
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | IndexingServiceException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to fork metadata with origin [" + origin + "] and key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting metadata for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
			if (registry.isLocked(key)) {
				throw new CoreServiceException("metadata with [" + key + "] is locked and cannot be modified.");
			}
			
			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}

			OrtolangObjectIdentifier tidentifier = registry.lookup(meta.getTarget());
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) || !tidentifier.getType().equals(Collection.OBJECT_TYPE) || !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			switch (tidentifier.getType()) {
				case Collection.OBJECT_TYPE:
					Collection collection = em.find(Collection.class, identifier.getId());
					if (collection == null) {
						throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
					}
					collection.removeMetadata(key);
					em.merge(collection);
					break;
				case DataObject.OBJECT_TYPE:
					DataObject object = em.find(DataObject.class, identifier.getId());
					if (object == null) {
						throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
					}
					object.removeMetadata(key);
					em.merge(object);
					break;
				case Link.OBJECT_TYPE:
					Link link = em.find(Link.class, identifier.getId());
					if (link == null) {
						throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
					}
					link.removeMetadata(key);
					em.merge(link);
					break;
			}

			registry.delete(key);

			indexing.remove(key);
			indexing.reindex(meta.getTarget());
			
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "delete"), "");
			notification.throwEvent(meta.getTarget(), caller, tidentifier.getType(), OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "remove-metadata"), "key=" + key);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | MembershipServiceException | AuthorisationServiceException e) {
			throw new CoreServiceException("unable to delete metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding metadata for target [" + target + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(target, subjects, "read");

			TypedQuery<MetadataObject> query = em.createNamedQuery("findMetadataObjectsForTarget", MetadataObject.class).setParameter("target", target);
			List<MetadataObject> mdos = query.getResultList();
			List<String> results = new ArrayList<String>();
			for (MetadataObject mdo : mdos) {
				String key = registry.lookup(mdo.getObjectIdentifier());
				results.add(key);
			}
			notification.throwEvent("", caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "find"), "target="
					+ target);
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException
				| IdentifierNotRegisteredException e) {
			throw new CoreServiceException("unable to find metadata for target [" + target + "]", e);
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
			for ( DataObject object : objects ) {
				object.setKey(registry.lookup(object.getObjectIdentifier()));
			}
			TypedQuery<MetadataObject> query2 = em.createNamedQuery("findMetadataObjectByBinaryHash", MetadataObject.class).setParameter("hash", hash);
			List<MetadataObject> objects2 = query2.getResultList();
			for ( MetadataObject mobject : objects2 ) {
				mobject.setKey(registry.lookup(mobject.getObjectIdentifier()));
			}
			List<OrtolangObject> oobjects = new ArrayList<OrtolangObject>();
			oobjects.addAll(objects);
			oobjects.addAll(objects2);
			return oobjects;
		} catch (Exception e) {
			throw new OrtolangException("unable to find an object for hash " + hash);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("system")
	public OrtolangIndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				if (object.getName() != null) {
					content.addContentPart(object.getName());
				}
				if (object.getDescription() != null) {
					content.addContentPart(object.getDescription());
				}
				if (object.getContentType() != null) {
					content.addContentPart(object.getContentType());
				}
				if (object.getPreview() != null) {
					content.addContentPart(object.getPreview());
				}
				try {
					content.addContentPart(binarystore.extract(object.getStreams().get("data-stream")));
				} catch (DataNotFoundException | BinaryStoreServiceException e) {
					logger.log(Level.WARNING, "unable to extract plain text for key : " + key, e);
				}
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

				try {
					content.addContentPart(binarystore.extract(metadata.getStream()));
				} catch (DataNotFoundException | BinaryStoreServiceException e) {
					logger.log(Level.WARNING, "unable to extract plain text for key : " + key, e);
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("system")
	public OrtolangIndexableSemanticContent getIndexableSemanticContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableSemanticContent content = new OrtolangIndexableSemanticContent();

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}

				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", object.getName()));
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}

				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", collection.getName()));
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				Link reference = em.find(Link.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}

				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", reference.getName()));
			}

			if (identifier.getType().equals(MetadataObject.OBJECT_TYPE)) {
				MetadataObject metadata = em.find(MetadataObject.class, identifier.getId());
				if (metadata == null) {
					throw new OrtolangException("unable to load metadata with id [" + identifier.getId() + "] from storage");
				}

				String subj = URIHelper.fromKey(key);
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#name", metadata.getName()));
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#contentType", metadata.getContentType()));
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#size", String.valueOf(metadata.getSize())));
				if(metadata.getFormat()!=null)
					content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#format", metadata.getFormat()));
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#target", URIHelper.fromKey(metadata.getTarget())));

				String log = "";
				try {
					//TODO provide a dedicated template service
					logger.log(Level.FINE, "rendering metadata content using template engine");
					VelocityContext ctx = new VelocityContext();
					ctx.put("self", URIHelper.fromKey(key));
					ctx.put("target", URIHelper.fromKey(metadata.getTarget()));
					InputStreamReader isr = new InputStreamReader(binarystore.get(metadata.getStream()));
					StringWriter writer = new StringWriter();
					Velocity.evaluate(ctx, writer, log, isr);
					logger.log(Level.FINEST, "rendered metadata : " + writer.toString());
					StringReader reader = new StringReader(writer.toString());
					Set<Triple> triplesContent = TripleHelper.extractTriples(reader, metadata.getContentType());
					for(Triple triple : triplesContent) {
						content.addTriple(triple);
					}
				} catch(TripleStoreServiceException te) {
					logger.log(Level.WARNING, "unable to parse metadata content for key: " + key);
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException | TripleStoreServiceException | BinaryStoreServiceException | DataNotFoundException e) {
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
