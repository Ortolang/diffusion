package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
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

@Local(CoreService.class)
@Stateless(name = CoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class CoreServiceBean implements CoreService {

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

	/* Workspace */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createWorkspace(String key, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating new workspace for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			String members = key + "-members";
			membership.createGroup(members, "Members of workspace " + name + "", "Members of a workspace have all permissions on workspace content");
			membership.addMemberInGroup(members, caller);

			String head = UUID.randomUUID().toString();
			Collection collection = new Collection();
			collection.setId(UUID.randomUUID().toString());
			collection.setName("root");
			collection.setRoot(true);
			collection.setClock(1);
			collection.setDescription("Root collection of workspace '" + name + "'");
			em.persist(collection);

			registry.register(head, collection.getObjectIdentifier(), caller);
			
			Map<String, List<String>> rules = new HashMap<String, List<String>>();
			rules.put(members, Arrays.asList("read", "create", "update", "delete", "download"));
			authorisation.createPolicy(head, members);
			authorisation.setPolicyRules(head, rules);

			Workspace workspace = new Workspace();
			workspace.setId(UUID.randomUUID().toString());
			workspace.setName(name);
			workspace.setType(type);
			workspace.setHead(head);
			workspace.setMembers(members);
			em.persist(workspace);

			registry.register(key, workspace.getObjectIdentifier(), caller);
			
			Map<String, List<String>> wsrules = new HashMap<String, List<String>>();
			wsrules.put(members, Arrays.asList("read"));
			authorisation.createPolicy(key, caller);
			authorisation.setPolicyRules(key, wsrules);

			notification.throwEvent(key, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "create"), "");
			notification.throwEvent(head, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while creating workspace", e);
			throw new CoreServiceException("unable to create workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Workspace readWorkspace(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading workspace for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setKey(key);
			em.detach(workspace);

			notification.throwEvent(key, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "read"), "");
			return workspace;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured while reading workspace", e);
			throw new CoreServiceException("unable to read workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding workspace for profile");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			List<String> groups = membership.getProfileGroups(profile);
			if (groups.size() == 0) {
				return Collections.emptyList();
			}

			List<String> keys = new ArrayList<String>();
			TypedQuery<Workspace> query = em.createNamedQuery("findWorkspaceByMember", Workspace.class).setParameter("groups", groups);
			List<Workspace> workspaces = query.getResultList();
			for (Workspace workspace : workspaces) {
				OrtolangObjectIdentifier identifier = workspace.getObjectIdentifier();
				try {
					keys.add(registry.lookup(identifier));
				} catch (IdentifierNotRegisteredException e) {
					logger.log(Level.SEVERE, "a workspace with an unregistered identifier has be found : " + identifier);
				}
			}

			notification.throwEvent("", caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "find"), "");
			return keys;
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
			logger.log(Level.SEVERE, "unexpected error occured during finding workspaces for profile", e);
			throw new CoreServiceException("unable to find workspaces for profile", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void snapshotWorkspace(String key, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "snapshoting workspace for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			if (name.equals("head")) {
				throw new CoreServiceException("head is reserved and cannot be used as snapshot name");
			}
			try {
				PathBuilder pname = PathBuilder.newInstance().path(name);
				if (pname.depth() > 1) {
					throw new CoreServiceException("snapshot name is invalid");
				}
			} catch (InvalidPathException e) {
				throw new CoreServiceException("snapshot name is invalid");
			}

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "update");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			if (!workspace.hasChanged()) {
				throw new CoreServiceException("unable to snapshot because workspace has no pending modifications since last snapshot");
			}
			if (workspace.findSnapshotByName(name) != null) {
				throw new CoreServiceException("the snapshot name '" + name + "' is already used in this workspace");
			}
			workspace.setKey(key);
			workspace.incrementClock();

			OrtolangObjectIdentifier hidentifier = registry.lookup(workspace.getHead());
			checkObjectType(hidentifier, Collection.OBJECT_TYPE);
			Collection collection = em.find(Collection.class, hidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load head collection with id [" + hidentifier.getId() + "] from storage");
			}
			collection.setKey(workspace.getHead());

			Collection clone = cloneCollection(workspace.getHead(), collection, workspace.getClock());

			workspace.addSnapshot(new SnapshotElement(name, collection.getKey()));
			workspace.setHead(clone.getKey());
			workspace.setChanged(false);
			em.merge(workspace);

			registry.update(key);

			notification.throwEvent(key, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "snapshot"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while snapshoting workspace", e);
			throw new CoreServiceException("unable to snapshot workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateWorkspace(String key, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating workspace for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "update");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setName(name);
			em.merge(workspace);

			registry.update(key);

			notification.throwEvent(key, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while updating workspace", e);
			throw new CoreServiceException("unable to update workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteWorkspace(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting workspace for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "delete");

			registry.delete(key);
			notification.throwEvent(key, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while deleting workspace", e);
			throw new CoreServiceException("unable to delete workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String resolveWorkspacePath(String workspace, String root, String path) throws CoreServiceException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "resolving into workspace [" + workspace + "] and root [" + root + "] path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "read");
			logger.log(Level.FINEST, "user [" + caller + "] has 'read' permission on the head collection of this workspace");

			String rroot = ws.getHead();
			if (root != null && root.length() > 0 && !root.equals("head")) {
				SnapshotElement element = ws.findSnapshotByName(root);
				if (element == null) {
					throw new InvalidPathException("root [" + root + "] does not exists");
				} else {
					rroot = element.getKey();
				}
			}

			if (npath.isRoot()) {
				return rroot;
			}

			Collection parent = readCollectionAtPath(rroot, ppath);
			logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}

			return element.getKey();
		} catch (KeyNotFoundException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | TreeBuilderException e) {
			logger.log(Level.SEVERE, "unexpected error occured during resolving path", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to resolve into workspace [" + workspace + "] path [" + path + "]", e);
		}
	}

	/* Collections */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "creating collection into workspace [" + workspace + "] at path [" + path + "]");
		String key = UUID.randomUUID().toString();
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("forbidden to create the root collection");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			logger.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			if (parent.containsElementName(npath.part())) {
				throw new InvalidPathException("path [" + npath.build() + "] already exists");
			}

			String id = UUID.randomUUID().toString();
			Collection collection = new Collection();
			collection.setId(id);
			collection.setName(npath.part());
			collection.setRoot(false);
			collection.setClock(ws.getClock());
			collection.setDescription(description);
			em.persist(collection);
			logger.log(Level.FINEST, "collection [" + key + "] created");

			registry.register(key, collection.getObjectIdentifier(), caller);
			
			authorisation.clonePolicy(key, ws.getHead());
			logger.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, collection.getName(), System.currentTimeMillis(), key));
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "collection [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException
				| AuthorisationServiceException | MembershipServiceException | TreeBuilderException e) {
			logger.log(Level.SEVERE, "unexpected error occured during collection creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create collection into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading collection with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier cidentifier = registry.lookup(key);
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			Collection collection = em.find(Collection.class, cidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			collection.setKey(key);
			em.detach(collection);

			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"), "");
			return collection;
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
			logger.log(Level.SEVERE, "unexpected error while reading collection", e);
			throw new CoreServiceException("unable to read collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "updating collection into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("unable to update the root collection");
			}

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");
			
			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");
			
			String current = resolveWorkspacePath(workspace, "head", npath.build());
			OrtolangObjectIdentifier cidentifier = registry.lookup(current);
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			Collection ccollection = em.find(Collection.class, cidentifier.getId());
			if (ccollection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			logger.log(Level.FINEST, "current collection loaded");
			
			if ( !description.equals(ccollection.getDescription()) ) {
				Collection collection = loadCollectionAtPath(ws.getHead(), npath, ws.getClock());
				logger.log(Level.FINEST, "collection loaded for path " + npath.build());
				
				collection.setDescription(description);
				em.merge(collection);
				registry.update(collection.getKey());
				logger.log(Level.FINEST, "collection updated");

				ws.setChanged(true);
				em.merge(ws);
				registry.update(ws.getKey());
				logger.log(Level.FINEST, "workspace set changed");

				notification.throwEvent(collection.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "update"),
						"");
			} else {
				logger.log(Level.FINEST, "no modification detected, doing nothing");
			}
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | TreeBuilderException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while updating collection", e);
			throw new CoreServiceException("unable to update collection into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveCollection(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "moving collection into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
		try {
			PathBuilder spath = PathBuilder.fromPath(source);
			if (spath.isRoot()) {
				throw new InvalidPathException("unable to move the root collection");
			}
			PathBuilder sppath = spath.clone().parent();

			PathBuilder dpath = PathBuilder.fromPath(destination);
			if (dpath.isRoot()) {
				throw new InvalidPathException("unable to move to the root collection");
			}
			PathBuilder dppath = dpath.clone().parent();

			if (dpath.equals(spath)) {
				throw new InvalidPathException("unable to move into the same path");
			}

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path " + spath.build() + " does not exists");
			}
			logger.log(Level.FINEST, "source collection element found for path " + spath.build() + ", key: " + selement.getKey());

			OrtolangObjectIdentifier scidentifier = registry.lookup(selement.getKey());
			checkObjectType(scidentifier, Collection.OBJECT_TYPE);
			Collection scollection = em.find(Collection.class, scidentifier.getId());
			if (scollection == null) {
				throw new TreeBuilderException("unable to load source collection with id [" + scidentifier.getId() + "] from storage");
			}
			scollection.setKey(selement.getKey());
			logger.log(Level.FINEST, "source collection exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			logger.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

			Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
			if (dparent.containsElementName(dpath.part())) {
				ctx.setRollbackOnly();
				throw new InvalidPathException("destination path " + dpath.build() + "already exists");
			}
			logger.log(Level.FINEST, "destination element does not exists, ok for creating it");
			if (!dpath.part().equals(spath.part())) {
				if (scollection.getClock() < ws.getClock()) {
					Collection clone = cloneCollection(ws.getHead(), scollection, ws.getClock());
					scollection = clone;
				}
				scollection.setName(dpath.part());
				em.merge(scollection);
				registry.update(scollection.getKey());
			}
			dparent.addElement(new CollectionElement(Collection.OBJECT_TYPE, scollection.getName(), System.currentTimeMillis(), scollection.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			logger.log(Level.FINEST, "collection [" + scollection.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(scollection.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "move"),
					"");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while moving collection", e);
			throw new CoreServiceException("unable to move collection into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "deleting collection into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("unable to delete the root collection");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			logger.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path " + npath.build() + " does not exists");
			}
			logger.log(Level.FINEST, "collection element found for path " + npath.build() + ", key: " + element.getKey());

			OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			Collection leaf = em.find(Collection.class, cidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			logger.log(Level.FINEST, "collection exists and loaded from storage");

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				logger.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}

				registry.delete(leaf.getKey());
			}

			notification.throwEvent(leaf.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while deleting collection", e);
			throw new CoreServiceException("unable to delete collection into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	/* Data Objects */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException,
			AccessDeniedException {
		logger.log(Level.FINE, "creating object into workspace [" + workspace + "] at path [" + path + "]");
		String key = UUID.randomUUID().toString();
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("forbidden to create an object at root level");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			logger.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			if (parent.containsElementName(npath.part())) {
				throw new InvalidPathException("path [" + npath.build() + "] already exists");
			}

			DataObject object = new DataObject();
			object.setId(UUID.randomUUID().toString());
			object.setName(npath.part());
			object.setDescription(description);
			if (hash != null && hash.length() > 0) {
				object.setSize(binarystore.size(hash));
				object.setContentType(binarystore.type(hash));
				object.setStream(hash);
			} else {
				object.setSize(0);
				object.setContentType("application/octet-stream");
				object.setStream("");
			}
			object.setClock(ws.getClock());
			object.setKey(key);
			em.persist(object);
			logger.log(Level.FINEST, "object [" + key + "] created");

			registry.register(key, object.getObjectIdentifier(), caller);
			
			authorisation.clonePolicy(key, ws.getHead());
			logger.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, object.getName(), System.currentTimeMillis(), key));
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "object [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException
				| AuthorisationServiceException | MembershipServiceException | TreeBuilderException | BinaryStoreServiceException | DataNotFoundException e) {
			logger.log(Level.SEVERE, "unexpected error occured during object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			object.setKey(key);
			em.detach(object);

			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"), "");
			return object;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured while reading object", e);
			throw new CoreServiceException("unable to read object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException,
			AccessDeniedException {
		logger.log(Level.FINE, "updating object into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");
			
			String current = resolveWorkspacePath(workspace, "head", npath.build());
			OrtolangObjectIdentifier cidentifier = registry.lookup(current);
			checkObjectType(cidentifier, DataObject.OBJECT_TYPE);
			DataObject cobject = em.find(DataObject.class, cidentifier.getId());
			if (cobject == null) {
				throw new CoreServiceException("unable to load object with id [" + cidentifier.getId() + "] from storage");
			}
			logger.log(Level.FINEST, "current object loaded");
			
			if ( !description.equals(cobject.getDescription()) || !hash.equals(cobject.getStream()) ) {
				Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
				logger.log(Level.FINEST, "parent collection loaded for path " + npath.build());

				CollectionElement element = parent.findElementByName(npath.part());
				if (element == null) {
					throw new InvalidPathException("path [" + npath.build() + "] does not exists");
				}
				logger.log(Level.FINEST, "object element found for name " + npath.part());
				if (!element.getType().equals(DataObject.OBJECT_TYPE)) {
					throw new InvalidPathException("path [" + npath.build() + "] is not a data object");
				}

				OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
				checkObjectType(identifier, DataObject.OBJECT_TYPE);
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				object.setKey(element.getKey());
				if (object.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
					parent.removeElement(element);
					CollectionElement celement = new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey());
					parent.addElement(celement);
					registry.update(parent.getKey());
					object = clone;
				}
				//TODO code to update last modification date of the parent 
//				} else {
//					parent.removeElement(element);
//					CollectionElement celement = new CollectionElement(CollectionElementType.COLLECTION, object.getName(), System.currentTimeMillis(), object.getKey());
//					parent.addElement(celement);
//				}
				object.setDescription(description);
				object.setKey(element.getKey());
				if (hash != null && hash.length() > 0) {
					object.setSize(binarystore.size(hash));
					object.setContentType(binarystore.type(hash));
					object.setStream(hash);
				} else {
					object.setSize(0);
					object.setContentType("application/octet-stream");
					object.setStream("");
				}
				em.merge(object);
				registry.update(object.getKey());
				logger.log(Level.FINEST, "object updated");

				notification.throwEvent(object.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"), "");
			} else {
				logger.log(Level.FINEST, "no changes detected with current object, nothing to do");
			}
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException | TreeBuilderException
				| BinaryStoreServiceException | DataNotFoundException | CloneException e) {
			logger.log(Level.SEVERE, "unexpected error occured while reading object", e);
			throw new CoreServiceException("unable to read object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveDataObject(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "moving object into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
		try {
			PathBuilder spath = PathBuilder.fromPath(source);
			if (spath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder sppath = spath.clone().parent();

			PathBuilder dpath = PathBuilder.fromPath(destination);
			if (dpath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder dppath = dpath.clone().parent();

			if (dpath.equals(spath)) {
				throw new InvalidPathException("unable to move into the same path");
			}

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path [" + spath.build() + "] does not exists");
			}
			logger.log(Level.FINEST, "source object element found for name " + spath.part());

			OrtolangObjectIdentifier soidentifier = registry.lookup(selement.getKey());
			checkObjectType(soidentifier, DataObject.OBJECT_TYPE);
			DataObject sobject = em.find(DataObject.class, soidentifier.getId());
			if (sobject == null) {
				throw new TreeBuilderException("unable to load source object with id [" + soidentifier.getId() + "] from storage");
			}
			sobject.setKey(selement.getKey());
			logger.log(Level.FINEST, "source object exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			logger.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

			Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
			if (dparent.containsElementName(dpath.part())) {
				ctx.setRollbackOnly();
				throw new InvalidPathException("destination path " + dpath.build() + "already exists");
			}
			logger.log(Level.FINEST, "destination element does not exists, creating it");
			if (!dpath.part().equals(spath.part())) {
				if (sobject.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), sobject, ws.getClock());
					sobject = clone;
				}
				sobject.setName(dpath.part());
				em.merge(sobject);
				registry.update(sobject.getKey());
			}
			dparent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, sobject.getName(), System.currentTimeMillis(), sobject.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			logger.log(Level.FINEST, "object [" + sobject.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(sobject.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "move"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while moving object", e);
			throw new CoreServiceException("unable to move object into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteDataObject(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "deleting object into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			logger.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			logger.log(Level.FINEST, "object element found for path " + npath.build() + ", key: " + element.getKey());

			OrtolangObjectIdentifier oidentifier = registry.lookup(element.getKey());
			checkObjectType(oidentifier, DataObject.OBJECT_TYPE);
			DataObject leaf = em.find(DataObject.class, oidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load object with id [" + oidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			logger.log(Level.FINEST, "object exists and loaded from storage");

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				logger.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}
				registry.delete(leaf.getKey());
			}

			notification.throwEvent(leaf.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while deleting object", e);
			throw new CoreServiceException("unable to delete object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	/* Link */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createLink(String workspace, String path, String target) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "create link into workspace [" + workspace + "] at path [" + path + "]");
		String key = UUID.randomUUID().toString();
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");
			authorisation.checkPermission(target, subjects, "read");
			logger.log(Level.FINEST, "user [" + caller + "] has 'read' permissions on the target");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			logger.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			if (parent.containsElementName(npath.part())) {
				throw new InvalidPathException("path [" + npath.build() + "] already exists");
			}

			Link link = new Link();
			link.setId(UUID.randomUUID().toString());
			link.setName(npath.part());
			link.setClock(ws.getClock());
			link.setTarget(target);
			em.persist(link);

			registry.register(key, link.getObjectIdentifier(), caller);
			
			authorisation.clonePolicy(key, ws.getHead());
			logger.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(Link.OBJECT_TYPE, link.getName(), System.currentTimeMillis(), key));
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "link [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException
				| AuthorisationServiceException | MembershipServiceException | TreeBuilderException e) {
			logger.log(Level.SEVERE, "unexpected error occured during link creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create link into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading link with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Link.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			Link link = em.find(Link.class, identifier.getId());
			if (link == null) {
				throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
			}
			link.setKey(key);

			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "read"), "");
			return link;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured while reading link", e);
			throw new CoreServiceException("unable to read link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveLink(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "moving link into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
		try {
			PathBuilder spath = PathBuilder.fromPath(source);
			if (spath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder sppath = spath.clone().parent();

			PathBuilder dpath = PathBuilder.fromPath(destination);
			if (dpath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder dppath = dpath.clone().parent();

			if (dpath.equals(spath)) {
				throw new InvalidPathException("unable to move into the same path");
			}

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path [" + spath.build() + "] does not exists");
			}
			logger.log(Level.FINEST, "source link element found for name " + spath.part());

			OrtolangObjectIdentifier slidentifier = registry.lookup(selement.getKey());
			checkObjectType(slidentifier, Link.OBJECT_TYPE);
			Link slink = em.find(Link.class, slidentifier.getId());
			if (slink == null) {
				throw new TreeBuilderException("unable to load source link with id [" + slidentifier.getId() + "] from storage");
			}
			slink.setKey(selement.getKey());
			logger.log(Level.FINEST, "source link exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			logger.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

			Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
			if (dparent.containsElementName(dpath.part())) {
				ctx.setRollbackOnly();
				throw new InvalidPathException("destination path [" + dpath.build() + "] already exists");
			}
			if (!dpath.part().equals(spath.part())) {
				if (slink.getClock() < ws.getClock()) {
					Link clone = cloneLink(ws.getHead(), slink, ws.getClock());
					slink = clone;
				}
				slink.setName(dpath.part());
				em.merge(slink);
				registry.update(slink.getKey());
			}
			dparent.addElement(new CollectionElement(Link.OBJECT_TYPE, slink.getName(), System.currentTimeMillis(), slink.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			logger.log(Level.FINEST, "link [" + slink.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(slink.getKey(), caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "move"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while moving link", e);
			throw new CoreServiceException("unable to move link into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteLink(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "deleting link into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			logger.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			logger.log(Level.FINEST, "link element found for name " + npath.part());

			OrtolangObjectIdentifier lidentifier = registry.lookup(element.getKey());
			checkObjectType(lidentifier, Link.OBJECT_TYPE);
			Link leaf = em.find(Link.class, lidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load link with id [" + lidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			logger.log(Level.FINEST, "link exists and loaded from storage");

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			logger.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			logger.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				logger.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}
				registry.delete(leaf.getKey());
			}

			notification.throwEvent(leaf.getKey(), caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error while deleting link", e);
			throw new CoreServiceException("unable to delete link into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding links for target [" + target + "]");
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
			logger.log(Level.SEVERE, "unexpected error occured during finding links for target", e);
			throw new CoreServiceException("unable to find link for target [" + target + "]", e);
		}
	}

	/* Metadatas */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException,
			AccessDeniedException {
		logger.log(Level.FINE, "creating metadataobject into workspace [" + workspace + "] for path [" + path + "] with name [" + name + "]");
		String key = UUID.randomUUID().toString();
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			logger.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			String tkey = ws.getHead();
			Collection parent = null;
			CollectionElement element = null;
			if (!npath.isRoot()) {
				parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
				logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
				element = parent.findElementByName(npath.part());
				if (element == null) {
					throw new InvalidPathException("path [" + npath.build() + "] does not exists");
				}
				logger.log(Level.FINEST, "collection element found for name " + npath.part());
				tkey = element.getKey();
			}

			OrtolangObjectIdentifier tidentifier = registry.lookup(tkey);
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			MetadataObject meta = new MetadataObject();
			meta.setId(UUID.randomUUID().toString());
			meta.setName(name);
			if (hash != null && hash.length() > 0) {
				meta.setSize(binarystore.size(hash));
				meta.setContentType(binarystore.type(hash));
				meta.setStream(hash);
			} else {
				meta.setSize(0);
				meta.setContentType("application/octet-stream");
				meta.setStream("");
			}
			meta.setTarget(tkey);
			meta.setFormat(format);
			meta.setKey(key);
			em.persist(meta);

			registry.register(key, meta.getObjectIdentifier(), caller);
			
			authorisation.clonePolicy(key, ws.getHead());

			switch (tidentifier.getType()) {
			case Collection.OBJECT_TYPE:
				Collection collection = em.find(Collection.class, tidentifier.getId());
				if (collection == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
				}
				for (MetadataElement mde : collection.getMetadatas()) {
					if (mde.getName().equals(name)) {
						ctx.setRollbackOnly();
						throw new CoreServiceException("a metadata object with name [" + name + "] already exists for collection at path [" + npath.build() + "]");
					}
				}
				if (collection.getClock() < ws.getClock()) {
					Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
					if (parent != null && element != null) {
						parent.removeElement(element);
						parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
						em.merge(parent);
						registry.update(parent.getKey());
					}
					collection = clone;
				}
				collection.addMetadata(new MetadataElement(name, key));
				em.merge(collection);
				break;
			case DataObject.OBJECT_TYPE:
				DataObject object = em.find(DataObject.class, tidentifier.getId());
				if (object == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
				}
				for (MetadataElement mde : object.getMetadatas()) {
					if (mde.getName().equals(name)) {
						ctx.setRollbackOnly();
						throw new CoreServiceException("a metadata object with name [" + name + "] already exists for object at path [" + npath.build() + "]");
					}
				}
				if (object.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
					parent.removeElement(element);
					parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
					em.merge(parent);
					registry.update(parent.getKey());
					object = clone;
				}
				object.addMetadata(new MetadataElement(name, key));
				em.merge(object);
				break;
			case Link.OBJECT_TYPE:
				Link link = em.find(Link.class, tidentifier.getId());
				if (link == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
				}
				for (MetadataElement mde : link.getMetadatas()) {
					if (mde.getName().equals(name)) {
						ctx.setRollbackOnly();
						throw new CoreServiceException("a metadata object with name [" + name + "] already exists for link at path [" + npath.build() + "]");
					}
				}
				if (link.getClock() < ws.getClock()) {
					Link clone = cloneLink(ws.getHead(), link, ws.getClock());
					parent.removeElement(element);
					parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
					em.merge(parent);
					registry.update(parent.getKey());
					link = clone;
				}
				link.addMetadata(new MetadataElement(name, key));
				em.merge(link);
				break;
			}

			registry.update(tkey);
			notification.throwEvent(tkey, caller, tidentifier.getType(), OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "add-metadata"), "key="
					+ key);
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"), "");

		} catch (KeyAlreadyExistsException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException
				| AuthorisationServiceException | MembershipServiceException | TreeBuilderException | BinaryStoreServiceException | DataNotFoundException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "]", e);
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
			em.detach(meta);

			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "read"), "");
			return meta;
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during reading metadata", e);
			throw new CoreServiceException("unable to read metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException,
			AccessDeniedException {
		logger.log(Level.FINE, "updating metadata content into workspace [" + workspace + "] for path [" + path + "] and name [" + name + "] and format ["+format+"]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			logger.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");
			
			String current = resolveWorkspacePath(workspace, "head", npath.build());
			OrtolangObjectIdentifier ctidentifier = registry.lookup(current);
			if (!ctidentifier.getType().equals(Link.OBJECT_TYPE) && !ctidentifier.getType().equals(Collection.OBJECT_TYPE) && !ctidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}
			MetadataElement cmdelement = null;
			switch (ctidentifier.getType()) {
				case Collection.OBJECT_TYPE:
					Collection collection = em.find(Collection.class, ctidentifier.getId());
					if (collection == null) {
						throw new CoreServiceException("unable to load collection with id [" + ctidentifier.getId() + "] from storage");
					}
					if (collection.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
					}
					cmdelement = collection.findMetadataByName(name);
					break;
				case DataObject.OBJECT_TYPE:
					DataObject object = em.find(DataObject.class, ctidentifier.getId());
					if (object == null) {
						throw new CoreServiceException("unable to load object with id [" + ctidentifier.getId() + "] from storage");
					}
					if (object.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
					}
					cmdelement = object.findMetadataByName(name);
					break;
				case Link.OBJECT_TYPE:
					Link link = em.find(Link.class, ctidentifier.getId());
					if (link == null) {
						throw new CoreServiceException("unable to load link with id [" + ctidentifier.getId() + "] from storage");
					}
					if (link.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
					}
					cmdelement = link.findMetadataByName(name);
					break;
			}
			if (cmdelement == null) {
				throw new CoreServiceException("unable to find current metadata target into workspace [" + workspace + "] for path [" + npath.build() + "] and name [" + name + "]");
			}
			OrtolangObjectIdentifier cidentifier = registry.lookup(cmdelement.getKey());
			checkObjectType(cidentifier, MetadataObject.OBJECT_TYPE);
			MetadataObject cmeta = em.find(MetadataObject.class, cidentifier.getId());
			if (cmeta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + cidentifier.getId() + "] from storage");
			}
			if ( !cmeta.equals(format) || !cmeta.equals(hash) ) {
				Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
				logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

				CollectionElement element = parent.findElementByName(npath.part());
				if (element == null) {
					throw new InvalidPathException("path [" + npath.build() + "] does not exists");
				}
				logger.log(Level.FINEST, "collection element found for name " + npath.part());

				OrtolangObjectIdentifier tidentifier = registry.lookup(element.getKey());
				if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
					throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
				}
				
				MetadataElement mdelement = null;
				switch (tidentifier.getType()) {
				case Collection.OBJECT_TYPE:
					Collection collection = em.find(Collection.class, tidentifier.getId());
					if (collection == null) {
						throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
					}
					if (collection.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
					}
					if (collection.getClock() < ws.getClock()) {
						Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
						parent.removeElement(element);
						parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
						collection = clone;
					}
					mdelement = collection.findMetadataByName(name);
					break;
				case DataObject.OBJECT_TYPE:
					DataObject object = em.find(DataObject.class, tidentifier.getId());
					if (object == null) {
						throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
					}
					if (object.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
					}
					if (object.getClock() < ws.getClock()) {
						DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
						parent.removeElement(element);
						parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
						object = clone;
					}
					mdelement = object.findMetadataByName(name);
					break;
				case Link.OBJECT_TYPE:
					Link link = em.find(Link.class, tidentifier.getId());
					if (link == null) {
						throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
					}
					if (link.findMetadataByName(name) == null) {
						throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
					}
					if (link.getClock() < ws.getClock()) {
						Link clone = cloneLink(ws.getHead(), link, ws.getClock());
						parent.removeElement(element);
						parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
						link = clone;
					}
					mdelement = link.findMetadataByName(name);
					break;
				}

				if (mdelement == null) {
					throw new CoreServiceException("unable to find metadata object into workspace [" + workspace + "] for path [" + npath.build() + "] and name [" + name + "]");
				}
				OrtolangObjectIdentifier identifier = registry.lookup(mdelement.getKey());
				checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
				MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
				if (meta == null) {
					throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
				}
				meta.setKey(mdelement.getKey());

				if (hash != null && hash.length() > 0) {
					meta.setSize(binarystore.size(hash));
					meta.setContentType(binarystore.type(hash));
					meta.setStream(hash);
				} else {
					meta.setSize(0);
					meta.setContentType("application/octet-stream");
					meta.setStream("");
				}
				meta.setFormat(format);
				em.merge(meta);

				registry.update(mdelement.getKey());

				notification.throwEvent(mdelement.getKey(), caller, MetadataObject.OBJECT_TYPE,
						OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "update"), "");
				notification.throwEvent(element.getKey(), caller, tidentifier.getType(),
						OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "update-metadata"), "key=" + mdelement.getKey());
			} else {
				logger.log(Level.FINEST, "no changes detected with current metadata object, nothing to do");
			}
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException | TreeBuilderException
				| BinaryStoreServiceException | DataNotFoundException | CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "] and name [" + name + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteMetadataObject(String workspace, String path, String name) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		logger.log(Level.FINE, "deleting metadataobject into workspace [" + workspace + "] for path [" + path + "] with name [" + name + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			logger.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			logger.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			logger.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			logger.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			logger.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			logger.log(Level.FINEST, "collection element found for name " + npath.part());
			OrtolangObjectIdentifier tidentifier = registry.lookup(element.getKey());
			if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
			}

			MetadataElement mdelement = null;
			switch (tidentifier.getType()) {
			case Collection.OBJECT_TYPE:
				Collection collection = em.find(Collection.class, tidentifier.getId());
				if (collection == null) {
					throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
				}
				if (collection.findMetadataByName(name) == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
				}
				if (collection.getClock() < ws.getClock()) {
					Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
					parent.removeElement(element);
					parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
					collection = clone;
				}
				mdelement = collection.findMetadataByName(name);
				collection.removeMetadata(mdelement);
				em.merge(collection);
				break;
			case DataObject.OBJECT_TYPE:
				DataObject object = em.find(DataObject.class, tidentifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
				}
				if (object.findMetadataByName(name) == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
				}
				if (object.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
					parent.removeElement(element);
					parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
					object = clone;
				}
				mdelement = object.findMetadataByName(name);
				object.removeMetadata(mdelement);
				em.merge(object);
				break;
			case Link.OBJECT_TYPE:
				Link link = em.find(Link.class, tidentifier.getId());
				if (link == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
				}
				if (link.findMetadataByName(name) == null) {
					ctx.setRollbackOnly();
					throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
				}
				if (link.getClock() < ws.getClock()) {
					Link clone = cloneLink(ws.getHead(), link, ws.getClock());
					parent.removeElement(element);
					parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey()));
					link = clone;
				}
				mdelement = link.findMetadataByName(name);
				link.removeMetadata(mdelement);
				em.merge(link);
				break;
			}

			if (mdelement == null) {
				throw new CoreServiceException("unable to find metadata object into workspace [" + workspace + "] for path [" + npath.build() + "] and name [" + name + "]");
			}
			OrtolangObjectIdentifier identifier = registry.lookup(mdelement.getKey());
			checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
			MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
			if (meta == null) {
				throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
			}
			meta.setKey(mdelement.getKey());

			registry.delete(mdelement.getKey());

			notification.throwEvent(mdelement.getKey(), caller, MetadataObject.OBJECT_TYPE,
					OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "delete"), "");
			notification.throwEvent(element.getKey(), caller, tidentifier.getType(),
					OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "remove-metadata"), "key=" + mdelement.getKey());
		} catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException | TreeBuilderException
				| CloneException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "]", e);
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
			logger.log(Level.SEVERE, "unexpected error occured during finding metadata", e);
			throw new CoreServiceException("unable to find metadata for target [" + target + "]", e);
		}
	}

	/* BinaryContent */

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public String put(InputStream data) throws CoreServiceException, DataCollisionException {
		logger.log(Level.FINE, "putting binary content in store");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			String hash = binarystore.put(data);
			notification.throwEvent(hash, caller, "binary-content", OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, "binary-content", "put"), "");
			return hash;
		} catch (NotificationServiceException | BinaryStoreServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during putting binary content", e);
			throw new CoreServiceException("unable to put binary content", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream preview(String key) throws CoreServiceException, DataNotFoundException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "preview content from store for object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, DataObject.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			DataObject object = em.find(DataObject.class, identifier.getId());
			if (object == null) {
				throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
			}
			String hash = object.getPreview();
			if (hash != null && hash.length() > 0) {
				InputStream stream = binarystore.get(hash);
				notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "preview"), "hash="
						+ hash);
				return stream;
			} else {
				throw new DataNotFoundException("there is no preview available for this data object");
			}
		} catch (NotificationServiceException | BinaryStoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during getting preview content", e);
			throw new CoreServiceException("unable to get preview content", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream download(String key) throws CoreServiceException, DataNotFoundException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "download content from store for object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			String hash;
			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				authorisation.checkPermission(key, subjects, "download");
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				hash = object.getStream();
			} else if (identifier.getType().equals(MetadataObject.OBJECT_TYPE)) {
				authorisation.checkPermission(key, subjects, "read");
				MetadataObject object = em.find(MetadataObject.class, identifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
				}
				hash = object.getStream();
			} else {
				throw new CoreServiceException("unable to find downloadable content for key [" + key + "]");
			}
			if (hash != null && hash.length() > 0) {
				InputStream stream = binarystore.get(hash);
				notification
						.throwEvent(key, caller, identifier.getType(), OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, identifier.getType(), "download"), "hash=" + hash);
				return stream;
			} else {
				throw new DataNotFoundException("there is no preview available for this data object");
			}
		} catch (NotificationServiceException | BinaryStoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during getting preview content", e);
			throw new CoreServiceException("unable to get preview content", e);
		}
	}

	/* Service */

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
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Workspace.OBJECT_TYPE)) {
				return readWorkspace(key);
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
		} catch (CoreServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
		try {
			TypedQuery<DataObject> query = em.createNamedQuery("findObjectByBinaryHash", DataObject.class).setParameter("hash", hash);
			List<DataObject> objects = query.getResultList();
			for (DataObject object : objects) {
				object.setKey(registry.lookup(object.getObjectIdentifier()));
			}
			TypedQuery<MetadataObject> query2 = em.createNamedQuery("findMetadataObjectByBinaryHash", MetadataObject.class).setParameter("hash", hash);
			List<MetadataObject> objects2 = query2.getResultList();
			for (MetadataObject mobject : objects2) {
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

			if (identifier.getType().equals(Workspace.OBJECT_TYPE)) {
				Workspace workspace = em.find(Workspace.class, identifier.getId());
				if (workspace == null) {
					throw new OrtolangException("unable to find workspace with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(workspace.getName());
				content.addContentPart(workspace.getType());
			}

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
					if (object.getStream() != null && object.getStream().length() > 0) {
						content.addContentPart(binarystore.extract(object.getStream()));
					}
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

			if (identifier.getType().equals(Workspace.OBJECT_TYPE)) {

			}

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", object.getName()));
				for(MetadataElement me : object.getMetadatas()) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#hasMetadata", URIHelper.fromKey(me.getKey())));
				}
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", collection.getName()));
				for(MetadataElement me : collection.getMetadatas()) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#hasMetadata", URIHelper.fromKey(me.getKey())));
				}
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				Link reference = em.find(Link.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", reference.getName()));
				for(MetadataElement me : reference.getMetadatas()) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#hasMetadata", URIHelper.fromKey(me.getKey())));
				}
			}

			if (identifier.getType().equals(MetadataObject.OBJECT_TYPE)) {
				MetadataObject metadata = em.find(MetadataObject.class, identifier.getId());
				if (metadata == null) {
					throw new OrtolangException("unable to load metadata with id [" + identifier.getId() + "] from storage");
				}

				String subj = URIHelper.fromKey(key);
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#name", metadata.getName()));
				content.addTriple(new Triple(subj, "http://www.ortolang.fr/2014/05/diffusion#metadataFormat", metadata.getFormat()));
				
				String log = "";
				try {
					// TODO provide a dedicated template service
					logger.log(Level.INFO, "rendering metadata content using template engine");
					//TODO Add others formats compatible with the triplestore
					if (metadata.getFormat().equals("rdf") && metadata.getStream() != null && metadata.getStream().length() > 0) {
						VelocityContext ctx = new VelocityContext();
						ctx.put("self", URIHelper.fromKey(key));
						ctx.put("target", URIHelper.fromKey(metadata.getTarget()));
						
						InputStreamReader isr = new InputStreamReader(binarystore.get(metadata.getStream()));
						StringWriter writer = new StringWriter();
						Velocity.evaluate(ctx, writer, log, isr);
						logger.log(Level.INFO, "rendered metadata : " + writer.toString());
						StringReader reader = new StringReader(writer.toString());
						Set<Triple> triplesContent = TripleHelper.extractTriples(reader, metadata.getContentType());
						for (Triple triple : triplesContent) {
							content.addTriple(triple);
							logger.log(Level.INFO, "Add triple : "+triple.getSubject()+":"+triple.getPredicate()+":"+triple.getObject());
						}
					}
				} catch (TripleStoreServiceException te) {
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

	/* ### Internal operations ### */

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Collection readCollectionAtPath(String root, PathBuilder path) throws TreeBuilderException {
		logger.log(Level.FINE, "Reading tree from root [" + root + "] to path [" + path.build() + "]");
		try {
			String[] parts = path.buildParts();
			Collection node = null;
			PathBuilder current = PathBuilder.newInstance();

			OrtolangObjectIdentifier ridentifier = registry.lookup(root);
			checkObjectType(ridentifier, Collection.OBJECT_TYPE);
			node = em.find(Collection.class, ridentifier.getId());
			if (node == null) {
				throw new TreeBuilderException("unable to load root collection with id [" + ridentifier.getId() + "] from storage");
			}
			node.setKey(root);
			if (!node.isRoot()) {
				throw new TreeBuilderException("root collection [" + root + "] is not flagged as a root collection");
			}

			for (int i = 0; i < parts.length; i++) {
				current.path(parts[i]);
				CollectionElement element = node.findElementByName(parts[i]);
				if (element == null) {
					throw new TreeBuilderException("unable to load path " + current.build() + ", parent collection " + node.getName() + " with key [" + node.getKey()
							+ " does not old an element named " + parts[i]);
				}
				OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
				checkObjectType(cidentifier, Collection.OBJECT_TYPE);
				node = em.find(Collection.class, cidentifier.getId());
				if (node == null) {
					throw new TreeBuilderException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
				}
				node.setKey(element.getKey());
				if (node.isRoot()) {
					logger.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
					throw new TreeBuilderException("Internal Problem : collection [" + parts[i] + "] is a root collection but is not a root node");
				}
			}

			return node;
		} catch (InvalidPathException | RegistryServiceException | KeyNotFoundException | CoreServiceException e) {
			throw new TreeBuilderException(e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Collection loadCollectionAtPath(String root, PathBuilder path, int clock) throws TreeBuilderException {
		logger.log(Level.FINE, "Loading tree from root [" + root + "] to path [" + path.build() + "] with clock [" + clock + "]");
		try {
			String[] parts = path.buildParts();
			Collection parent = null;
			Collection leaf = null;
			PathBuilder current = PathBuilder.newInstance();

			OrtolangObjectIdentifier ridentifier = registry.lookup(root);
			checkObjectType(ridentifier, Collection.OBJECT_TYPE);
			leaf = em.find(Collection.class, ridentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load root collection with id [" + ridentifier.getId() + "] from storage");
			}
			leaf.setKey(root);
			if (!leaf.isRoot()) {
				throw new TreeBuilderException("root collection [" + root + "] is not flagged as a root collection");
			}
			if (leaf.getClock() < clock) {
				logger.log(Level.SEVERE, "WRONG CLOCK found for root collection key [ " + root + "]");
				throw new TreeBuilderException("root collection [" + root + "] clock is not good");
			}

			for (int i = 0; i < parts.length; i++) {
				parent = leaf;
				current.path(parts[i]);
				CollectionElement element = parent.findElementByName(parts[i]);
				if (element == null) {
					throw new TreeBuilderException("unable to load path " + current.build() + ", parent collection " + parent.getName() + " with key [" + parent.getKey()
							+ " does not old an element named " + parts[i]);
				}
				OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
				checkObjectType(cidentifier, Collection.OBJECT_TYPE);
				leaf = em.find(Collection.class, cidentifier.getId());
				if (leaf == null) {
					throw new TreeBuilderException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
				}
				leaf.setKey(element.getKey());
				if (leaf.isRoot()) {
					logger.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
					throw new TreeBuilderException("Internal Problem : collection [" + parts[i] + "] is a root collection but is not a root node");
				}
				if (leaf.getClock() < clock) {
					Collection clone = cloneCollection(root, leaf, clock);
					parent.removeElement(element);
					CollectionElement celement = new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getKey());
					parent.addElement(celement);
					registry.update(parent.getKey());
					leaf = clone;
				}
			}

			return leaf;
		} catch (InvalidPathException | RegistryServiceException | KeyNotFoundException | CoreServiceException | CloneException e) {
			throw new TreeBuilderException(e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Collection cloneCollection(String root, Collection origin, int clock) throws CloneException {
		logger.log(Level.FINE, "cloning collection for origin [" + origin.getKey() + "]");
		try {
			String key = UUID.randomUUID().toString();

			Collection clone = new Collection();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(origin.getName());
			clone.setDescription(origin.getDescription());
			clone.setRoot(origin.isRoot());
			clone.setClock(clock);
			Set<CollectionElement> elements = new HashSet<CollectionElement>();
			elements.addAll(origin.getElements());
			clone.setElements(elements);
			Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
			for (MetadataElement mde : origin.getMetadatas()) {
				MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
				metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
			}
			clone.setMetadatas(metadatas);
			clone.setKey(key);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | KeyNotFoundException | AuthorisationServiceException e) {
			throw new CloneException("unable to clone collection with origin key [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private DataObject cloneDataObject(String root, DataObject origin, int clock) throws CloneException {
		logger.log(Level.FINE, "cloning object for origin [" + origin.getKey() + "]");
		try {
			String key = UUID.randomUUID().toString();

			DataObject clone = new DataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(origin.getName());
			clone.setDescription(origin.getDescription());
			clone.setSize(origin.getSize());
			clone.setContentType(origin.getContentType());
			clone.setStream(origin.getStream());
			clone.setPreview(origin.getPreview());
			clone.setClock(clock);
			Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
			for (MetadataElement mde : origin.getMetadatas()) {
				MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
				metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
			}
			clone.setMetadatas(metadatas);
			clone.setKey(key);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException e) {
			throw new CloneException("unable to clone object with origin [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Link cloneLink(String root, Link origin, int clock) throws CloneException {
		logger.log(Level.FINE, "cloning link for origin [" + origin + "]");
		try {
			String key = UUID.randomUUID().toString();

			Link clone = new Link();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(origin.getName());
			clone.setTarget(origin.getTarget());
			clone.setClock(clock);
			Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
			for (MetadataElement mde : origin.getMetadatas()) {
				MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
				metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
			}
			clone.setMetadatas(metadatas);
			clone.setKey(key);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException e) {
			throw new CloneException("unable to clone link with origin [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private MetadataObject cloneMetadataObject(String root, String origin, String target) throws CloneException {
		logger.log(Level.FINE, "clone metadata for origin [" + origin + "], target [" + target + "]");
		try {
			String key = UUID.randomUUID().toString();

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
			clone.setKey(key);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | CoreServiceException | KeyNotFoundException
				| KeyAlreadyExistsException e) {
			throw new CloneException("unable to clone metadata with origin [" + origin + "] and target [" + target + "]", e);
		}
	}

}