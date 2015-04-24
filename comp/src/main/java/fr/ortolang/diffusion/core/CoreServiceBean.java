package fr.ortolang.diffusion.core;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.*;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.*;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicyTemplate;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;
import fr.ortolang.diffusion.store.triple.IndexableSemanticContent;
import fr.ortolang.diffusion.store.triple.Triple;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;
import fr.ortolang.diffusion.store.triple.URIHelper;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

@Local(CoreService.class)
@Stateless(name = CoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class CoreServiceBean implements CoreService {

	private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { Workspace.OBJECT_TYPE, DataObject.OBJECT_TYPE, Collection.OBJECT_TYPE, Link.OBJECT_TYPE, MetadataObject.OBJECT_TYPE };
	private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { Workspace.OBJECT_TYPE, "read,update,delete,snapshot" }, { DataObject.OBJECT_TYPE, "read,update,delete,download" },
			{ Collection.OBJECT_TYPE, "read,update,delete,download" }, { Link.OBJECT_TYPE, "read,update,delete" }, { MetadataObject.OBJECT_TYPE, "read,update,delete,download" } };

	@EJB
	private RegistryService registry;
	@EJB
	private BinaryStoreService binarystore;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexingService indexing;
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

	public IndexingService getIndexingService() {
		return indexing;
	}

	public void setIndexingService(IndexingService indexing) {
		this.indexing = indexing;
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
	public Workspace createWorkspace(String wskey, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		WorkspaceAlias alias = new WorkspaceAlias();
		em.persist(alias);
		return createWorkspace(wskey, alias.getValue(), name, type);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Workspace createWorkspace(String wskey, String alias, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		LOGGER.log(Level.FINE, "creating workspace [" + wskey + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);

			String members = UUID.randomUUID().toString();
			membership.createGroup(members, name + "'s Members", "Members of a workspace have all permissions on workspace content");
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
			registry.itemify(head);

			Map<String, List<String>> rules = new HashMap<String, List<String>>();
			rules.put(members, Arrays.asList("read", "create", "update", "delete", "download"));
			authorisation.createPolicy(head, members);
			authorisation.setPolicyRules(head, rules);

			List<Workspace> results = em.createNamedQuery("findWorkspaceByAlias", Workspace.class).setParameter("alias", alias).getResultList();
			if (results.size() > 0) {
				ctx.setRollbackOnly();
				throw new CoreServiceException("a workspace with alias [" + alias + "] already exists in storage");
			}
			PathBuilder palias;
			try {
				palias = PathBuilder.fromPath(alias);
				if (palias.isRoot() || palias.depth() > 1) {
					throw new InvalidPathException("incorrect depth for an alias path");
				}
			} catch (InvalidPathException e) {
				LOGGER.log(Level.SEVERE, "invalid alias for workspace", e);
				throw new CoreServiceException("alias is invalid", e);
			}
			String id = UUID.randomUUID().toString();
			Workspace workspace = new Workspace();
			workspace.setId(id);
			workspace.setKey(wskey);
			workspace.setAlias(palias.part());
			workspace.setName(name);
			workspace.setType(type);
			workspace.setHead(head);
			workspace.setChanged(true);
			workspace.setMembers(members);
			em.persist(workspace);

			registry.register(wskey, workspace.getObjectIdentifier(), caller);

			Map<String, List<String>> wsrules = new HashMap<String, List<String>>();
			wsrules.put(members, Arrays.asList("read"));
			authorisation.createPolicy(wskey, caller);
			authorisation.setPolicyRules(wskey, wsrules);
			
			notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "create"));
			notification.throwEvent(head, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"));

			return workspace;
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw e;
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured while creating workspace", e);
			throw new CoreServiceException("unable to create workspace with key [" + wskey + "]", e);
		} 
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Workspace readWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "reading workspace [" + wskey + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "read");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setKey(wskey);
			return workspace;
		} catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while reading workspace", e);
			throw new CoreServiceException("unable to read workspace with key [" + wskey + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "finding workspace for profile");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);

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
					LOGGER.log(Level.SEVERE, "a workspace with an unregistered identifier has be found : " + identifier);
				}
			}

			notification.throwEvent("", caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "find"));
			return keys;
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during finding workspaces for profile", e);
			throw new CoreServiceException("unable to find workspaces for profile", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void snapshotWorkspace(String wskey, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "snapshoting workspace [" + wskey + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "update");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			if (!workspace.hasChanged()) {
				throw new CoreServiceException("unable to snapshot because workspace has no pending modifications since last snapshot");
			}
			workspace.setKey(wskey);
			workspace.incrementClock();

			if (name.equals(Workspace.HEAD)) {
				throw new CoreServiceException(name + " is reserved and cannot be used as snapshot name");
			}
			try {
				PathBuilder pname = PathBuilder.newInstance().path(name);
				if (pname.depth() > 1) {
					throw new CoreServiceException("snapshot name is invalid: " + name);
				}
			} catch (InvalidPathException e) {
				throw new CoreServiceException("snapshot name is invalid: " + name);
			}
			if (workspace.findSnapshotByName(name) != null) {
				throw new CoreServiceException("the snapshot name '" + name + "' is already used in this workspace");
			}

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

			registry.update(wskey);

			notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "snapshot"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured while snapshoting workspace", e);
			throw new CoreServiceException("unable to snapshot workspace with key [" + wskey + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateWorkspace(String wskey, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating workspace [" + wskey + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "update");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setName(name);
			em.merge(workspace);

			registry.update(wskey);
			
			notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured while updating workspace", e);
			throw new CoreServiceException("unable to update workspace with key [" + wskey + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting workspace [" + wskey + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "delete");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setAlias(null);
			em.merge(workspace);

			membership.deleteGroup(workspace.getMembers());
			registry.delete(wskey);
			
			notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured while deleting workspace", e);
			throw new CoreServiceException("unable to delete workspace with key [" + wskey + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String resolveWorkspaceAlias(String alias) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "finding workspace for alias:" + alias );
		try {
			TypedQuery<Workspace> query = em.createNamedQuery("findWorkspaceByAlias", Workspace.class).setParameter("alias", alias);
			Workspace workspace = query.getSingleResult();
			String wskey = registry.lookup(workspace.getObjectIdentifier());
			return wskey;
		} catch (RegistryServiceException | IdentifierNotRegisteredException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during resolving workspace alias: " + alias, e);
			throw new CoreServiceException("unable to resolve workspace alias: " + alias, e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String resolveWorkspacePath(String wskey, String root, String path) throws CoreServiceException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "resolving into workspace [" + wskey + "] and root [" + root + "] path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(wskey);
			LOGGER.log(Level.FINEST, "workspace loaded");

			String rroot = ws.getHead();
			if (root != null && root.length() > 0 && !root.equals(Workspace.HEAD)) {
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
			LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}

			return element.getKey();
		} catch ( TreeBuilderException e ) {
			LOGGER.log(Level.FINE, "unable to resolve path [" + path + "] : " + e.getMessage());
			throw new InvalidPathException("path [" + path + "] does not exists in workspace [" + wskey + "]");
		} catch (KeyNotFoundException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during resolving path", e);
			throw new CoreServiceException("unable to resolve into workspace [" + wskey + "] path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String resolveWorkspaceMetadata(String wskey, String root, String path, String name) throws CoreServiceException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "resolving into workspace [" + wskey + "] and root [" + root + "] metadata with name [" + name + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);

			String key = resolveWorkspacePath(wskey, root, path);
			OrtolangObjectIdentifier ctidentifier = registry.lookup(key);
			if (!ctidentifier.getType().equals(Link.OBJECT_TYPE) && !ctidentifier.getType().equals(Collection.OBJECT_TYPE) && !ctidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			MetadataElement cmdelement = null;
			switch (ctidentifier.getType()) {
			case Collection.OBJECT_TYPE:
				Collection collection = em.find(Collection.class, ctidentifier.getId());
				if (collection == null) {
					throw new CoreServiceException("unable to load collection with id [" + ctidentifier.getId() + "] from storage");
				}
				cmdelement = collection.findMetadataByName(name);
				break;
			case DataObject.OBJECT_TYPE:
				DataObject object = em.find(DataObject.class, ctidentifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + ctidentifier.getId() + "] from storage");
				}
				cmdelement = object.findMetadataByName(name);
				break;
			case Link.OBJECT_TYPE:
				Link link = em.find(Link.class, ctidentifier.getId());
				if (link == null) {
					throw new CoreServiceException("unable to load link with id [" + ctidentifier.getId() + "] from storage");
				}
				cmdelement = link.findMetadataByName(name);
				break;
			}
			if (cmdelement == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			return cmdelement.getKey();
		} catch (KeyNotFoundException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during resolving metadata", e);
			throw new CoreServiceException("unable to resolve into workspace [" + wskey + "] metadata name [" + name + "] at [" + path + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Set<String> buildWorkspaceReviewList(String wskey, String snapshot) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "building review list for workspace [" + wskey + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "read");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			if ( !workspace.containsSnapshotName(snapshot) ) {
				throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + snapshot);
			}
			String root = workspace.findSnapshotByName(snapshot).getKey();
			
			Set<String> keys = new HashSet<String>();
			systemListCollectionKeys(root, keys);
			return keys;
		} catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during building workspace review list", e);
			throw new CoreServiceException("unexpected error while trying to build workspace review list", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Map<String, Map<String, List<String>>> buildWorkspacePublicationMap(String wskey, String snapshot) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "building publication map for workspace [" + wskey + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			authorisation.checkPermission(wskey, subjects, "read");

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			if ( !workspace.containsSnapshotName(snapshot) ) {
				throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + snapshot);
			}
			String root = workspace.findSnapshotByName(snapshot).getKey();
			
			Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
			AuthorisationPolicyTemplate defaultTemplate = authorisation.getPolicyTemplate(AuthorisationPolicyTemplate.DEFAULT);
			Map<String, String> aclParams = new HashMap<String, String> ();
			aclParams.put("${workspace.members}", workspace.getMembers());
			builtPublicationMap(root, map, authorisation.getPolicyRules(defaultTemplate.getTemplate()), aclParams);
			return map;
		} catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | OrtolangException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during building workspace publication map", e);
			throw new CoreServiceException("unexpected error while trying to build workspace publication map", e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private void builtPublicationMap(String key, Map<String, Map<String, List<String>>> map, Map<String, List<String>> current, Map<String, String> params) throws KeyNotFoundException, AccessDeniedException, CoreServiceException, OrtolangException {
		Object object = findObject(key);
		if (object instanceof MetadataSource) {
			MetadataElement mde = ((MetadataSource)object).findMetadataByName(MetadataFormat.ACL);
			if ( mde != null ) {
				LOGGER.log(Level.FINE, "ACL metadata found, load json, find policy template and render it...");
				MetadataObject md = readMetadataObject(mde.getKey());
				try {
					JsonReader reader = Json.createReader(binarystore.get(md.getStream()));
					JsonObject json = reader.readObject();
					String template = json.getString("template");
					reader.close();
					AuthorisationPolicyTemplate policy = authorisation.getPolicyTemplate(template);
					Map<String, List<String>> rules = authorisation.getPolicyRules(policy.getTemplate());
					Map<String, List<String>> filtered = new HashMap<String, List<String>> ();
					for ( Entry<String, List<String>> entry : rules.entrySet() ) {
						if ( params.containsKey(entry.getKey()) ) {
							filtered.put(params.get(entry.getKey()), entry.getValue());
						} else {
							filtered.put(entry.getKey(), entry.getValue());
						}
					}
					current = filtered;
				} catch ( AuthorisationServiceException | BinaryStoreServiceException | DataNotFoundException e ) {
					LOGGER.log(Level.SEVERE, "unable to read acl metadata", e);
				}
			}
		}
		map.put(key, current);
		if (object instanceof MetadataSource) {
			for (MetadataElement element : ((MetadataSource)object).getMetadatas()) {
				map.put(element.getKey(), current);
			}
		}
		if (object instanceof Collection) {
			for (CollectionElement element : ((Collection)object).getElements()) {
				builtPublicationMap(element.getKey(), map, current, params);
			}
		}
	}
	
	public String findWorkspaceLatestPublishedSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "find workspace [" + wskey + "] latest published snapshot");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);

			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			workspace.setKey(wskey);
			
			String current = workspace.getHead();
			boolean found = false;
			while (!found && current != null) {
				String parent = registry.getParent(current);
				if ( parent != null && registry.getPublicationStatus(parent).equals(OrtolangObjectState.Status.PUBLISHED.value()) ) {
					found = true;
				}
				current = parent;
			}
			
			if ( current != null ) {
				SnapshotElement snapshot = workspace.findSnapshotByKey(current); 
				if ( snapshot != null ) {
					return snapshot.getName();
				}
			}
			
			return null;
		} catch (RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while finding workspace latest published snapshot", e);
			throw new CoreServiceException("unable to find latest published snapshot for workspace with key [" + wskey + "]", e);
		}
	}

	/* Collections */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCollection(String wskey, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		String key = UUID.randomUUID().toString();
		try {
			createCollection(wskey, key, path, description);
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createCollection(String wskey, String key, String path, String description) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException,
			AccessDeniedException {
		LOGGER.log(Level.FINE, "creating collection with key [" + key + "] into workspace [" + wskey + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("forbidden to create the root collection");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(wskey);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

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
			LOGGER.log(Level.FINEST, "collection [" + key + "] created");

			registry.register(key, collection.getObjectIdentifier(), caller);
			indexing.index(key);
			
			authorisation.clonePolicy(key, ws.getHead());
			LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, collection.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, key));
			em.merge(parent);
			registry.update(parent.getKey());
			LOGGER.log(Level.FINEST, "collection [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"));
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder(2).addArgument("oKey", key).addArgument("path", path);
			notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"), argumentsBuilder.build());
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException | TreeBuilderException | IndexingServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during collection creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create collection into workspace [" + wskey + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "reading collection with key [" + key + "]");
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
			
			notification.throwEvent(key, caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"));
			return collection;
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error while reading collection", e);
			throw new CoreServiceException("unable to read collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectSize getSize(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "calculating size for object with key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			OrtolangObjectIdentifier cidentifier = registry.lookup(key);
			if (!cidentifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + cidentifier + " does not refer to service " + getServiceName());
			}
			OrtolangObjectSize ortolangObjectSize = new OrtolangObjectSize();
			switch (cidentifier.getType()) {
			case DataObject.OBJECT_TYPE: {
				authorisation.checkPermission(key, subjects, "read");
				DataObject dataObject = em.find(DataObject.class, cidentifier.getId());
				ortolangObjectSize.addElement(DataObject.OBJECT_TYPE, dataObject.getSize());
				break;
			}
			case Collection.OBJECT_TYPE: {
				ortolangObjectSize = getCollectionSize(key, cidentifier, ortolangObjectSize, subjects);
				break;
			}
			case Workspace.OBJECT_TYPE: {
				authorisation.checkPermission(key, subjects, "read");
				Workspace workspace = em.find(Workspace.class, cidentifier.getId());
				ortolangObjectSize = getCollectionSize(workspace.getHead(), registry.lookup(workspace.getHead()), ortolangObjectSize, subjects);
				for (SnapshotElement snapshotElement : workspace.getSnapshots()) {
					ortolangObjectSize = getCollectionSize(snapshotElement.getKey(), registry.lookup(snapshotElement.getKey()), ortolangObjectSize, subjects);
				}
				break;
			}
			}
			return ortolangObjectSize;
		} catch (CoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error while calculating object size", e);
			throw new OrtolangException("unable to calculate size for object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, InvalidPathException {
		LOGGER.log(Level.FINE, "reading collection with key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			OrtolangObjectIdentifier cidentifier = registry.lookup(key);
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			authorisation.checkPermission(key, subjects, "read");

			Collection collection = em.find(Collection.class, cidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			collection.setKey(key);
			
			PathBuilder pathTarget = PathBuilder.fromPath(path);
			PathBuilder parentTarget = pathTarget.clone().parent();

			Collection parent = readCollectionAtPath(key, parentTarget);

			String partTarget = pathTarget.part();

			CollectionElement element = parent.findElementByName(partTarget);
			if (element == null) {
				throw new TreeBuilderException("unable to load path " + path + ", parent collection " + parent.getName() + " with key [" + parent.getKey() + " does not old an element named "
						+ partTarget);
			}

			return element.getKey();
		} catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException | TreeBuilderException e) {
			LOGGER.log(Level.SEVERE, "unexpected error while reading collection", e);
			throw new CoreServiceException("unable to resolve path " + path + " from collection with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating collection into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("unable to update the root collection");
			}

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			String current = resolveWorkspacePath(workspace, Workspace.HEAD, npath.build());
			OrtolangObjectIdentifier cidentifier = registry.lookup(current);
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			Collection ccollection = em.find(Collection.class, cidentifier.getId());
			if (ccollection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			LOGGER.log(Level.FINEST, "current collection loaded");

			if (!description.equals(ccollection.getDescription())) {
				Collection collection = loadCollectionAtPath(ws.getHead(), npath, ws.getClock());
				LOGGER.log(Level.FINEST, "collection loaded for path " + npath.build());

				collection.setDescription(description);
				em.merge(collection);
				registry.update(collection.getKey());
				indexing.reindex(collection.getKey());
				LOGGER.log(Level.FINEST, "collection updated");

				ws.setChanged(true);
				em.merge(ws);
				registry.update(ws.getKey());
				LOGGER.log(Level.FINEST, "workspace set changed");

				notification.throwEvent(collection.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "update"));
			} else {
				LOGGER.log(Level.FINEST, "no modification detected, doing nothing");
			}
		} catch (KeyLockedException | NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | TreeBuilderException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while updating collection", e);
			throw new CoreServiceException("unable to update collection into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveCollection(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "moving collection into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
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
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path " + spath.build() + " does not exists");
			}
			LOGGER.log(Level.FINEST, "source collection element found for path " + spath.build() + ", key: " + selement.getKey());

			OrtolangObjectIdentifier scidentifier = registry.lookup(selement.getKey());
			checkObjectType(scidentifier, Collection.OBJECT_TYPE);
			Collection scollection = em.find(Collection.class, scidentifier.getId());
			if (scollection == null) {
				throw new TreeBuilderException("unable to load source collection with id [" + scidentifier.getId() + "] from storage");
			}
			scollection.setKey(selement.getKey());
			LOGGER.log(Level.FINEST, "source collection exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			
			LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

			Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
			if (dparent.containsElementName(dpath.part())) {
				ctx.setRollbackOnly();
				throw new InvalidPathException("destination path " + dpath.build() + "already exists");
			}
			LOGGER.log(Level.FINEST, "destination element does not exists, ok for creating it");
			if (!dpath.part().equals(spath.part())) {
				if (scollection.getClock() < ws.getClock()) {
					Collection clone = cloneCollection(ws.getHead(), scollection, ws.getClock());
					scollection = clone;
				}
				scollection.setName(dpath.part());
				em.merge(scollection);
				registry.update(scollection.getKey());
			}
			dparent.addElement(new CollectionElement(Collection.OBJECT_TYPE, scollection.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, scollection.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			
			LOGGER.log(Level.FINEST, "collection [" + scollection.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(scollection.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "move"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while moving collection", e);
			throw new CoreServiceException("unable to move collection into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, CollectionNotEmptyException {
		deleteCollection(workspace, path, false);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteCollection(String workspace, String path, boolean force) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
			CollectionNotEmptyException {
		LOGGER.log(Level.FINE, "deleting collection into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("unable to delete the root collection");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path " + npath.build() + " does not exists");
			}
			LOGGER.log(Level.FINEST, "collection element found for path " + npath.build() + ", key: " + element.getKey());

			OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
			checkObjectType(cidentifier, Collection.OBJECT_TYPE);
			Collection leaf = em.find(Collection.class, cidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			LOGGER.log(Level.FINEST, "collection exists and loaded from storage");

			if (!leaf.isEmpty() && !force) {
				throw new CollectionNotEmptyException("collection at path: [" + path + "] is not empty");
			}

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			
			LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}
				registry.delete(leaf.getKey());
				indexing.remove(parent.getKey());
			}

			deleteCollectionContent(leaf, ws.getClock());

			notification.throwEvent(leaf.getKey(), caller, Collection.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while deleting collection", e);
			throw new CoreServiceException("unable to delete collection into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	/* Data Objects */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		String key = UUID.randomUUID().toString();
		try {
			createDataObject(workspace, key, path, description, hash);
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createDataObject(String workspace, String key, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException,
			InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "create data objetc with key [" + key + "] into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("forbidden to create an object at root level");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			if (parent.containsElementName(npath.part())) {
				throw new InvalidPathException("path [" + npath.build() + "] already exists");
			}

			DataObject object = new DataObject();
			object.setId(UUID.randomUUID().toString());
			object.setName(npath.part());
			object.setDescription(description);
			if (hash != null && hash.length() > 0) {
				object.setSize(binarystore.size(hash));
				object.setMimeType(binarystore.type(hash));
				object.setStream(hash);
			} else {
				object.setSize(0);
				object.setMimeType("application/octet-stream");
				object.setStream("");
			}
			object.setClock(ws.getClock());
			object.setKey(key);
			em.persist(object);
			LOGGER.log(Level.FINEST, "object [" + key + "] created");

			registry.register(key, object.getObjectIdentifier(), caller);
			indexing.index(key);

			authorisation.clonePolicy(key, ws.getHead());
			LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, object.getName(), System.currentTimeMillis(), object.getSize(), object.getMimeType(), key));
			em.merge(parent);
			registry.update(parent.getKey());
			
			LOGGER.log(Level.FINEST, "object [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder(2).addArgument("wskey", ws.getKey()).addArgument("members", ws.getMembers());
			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), argumentsBuilder.build());
			ArgumentsBuilder argumentsBuilder2 = new ArgumentsBuilder(2).addArgument("oKey", key).addArgument("path", path);
			notification.throwEvent(ws.getKey(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"), argumentsBuilder2.build());
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException | TreeBuilderException | BinaryStoreServiceException | DataNotFoundException | IndexingServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred during object creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "reading object with key [" + key + "]");
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

			notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"));
			return object;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while reading object", e);
			throw new CoreServiceException("unable to read object with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating object into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			String current = resolveWorkspacePath(workspace, Workspace.HEAD, npath.build());
			OrtolangObjectIdentifier cidentifier = registry.lookup(current);
			checkObjectType(cidentifier, DataObject.OBJECT_TYPE);
			DataObject cobject = em.find(DataObject.class, cidentifier.getId());
			if (cobject == null) {
				throw new CoreServiceException("unable to load object with id [" + cidentifier.getId() + "] from storage");
			}
			LOGGER.log(Level.FINEST, "current object loaded");

			if (!description.equals(cobject.getDescription()) || !hash.equals(cobject.getStream())) {
				Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
				LOGGER.log(Level.FINEST, "parent collection loaded for path " + npath.build());

				CollectionElement element = parent.findElementByName(npath.part());
				if (element == null) {
					throw new InvalidPathException("path [" + npath.build() + "] does not exists");
				}
				LOGGER.log(Level.FINEST, "object element found for name " + npath.part());
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
				object.setDescription(description);
				object.setKey(element.getKey());
				if (hash != null && hash.length() > 0) {
					object.setSize(binarystore.size(hash));
					object.setMimeType(binarystore.type(hash));
					object.setStream(hash);
				} else {
					object.setSize(0);
					object.setMimeType("application/octet-stream");
					object.setStream("");
				}
				if (object.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
					parent.removeElement(element);
					CollectionElement celement = new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey());
					parent.addElement(celement);
					registry.update(parent.getKey());
					object = clone;
				} else {
					parent.removeElement(element);
					CollectionElement celement = new CollectionElement(DataObject.OBJECT_TYPE, object.getName(), System.currentTimeMillis(), object.getSize(), object.getMimeType(), object.getKey());
					parent.addElement(celement);
				}
				em.merge(parent);
				em.merge(object);
				registry.update(object.getKey());
				indexing.reindex(object.getKey());
				LOGGER.log(Level.FINEST, "object updated");

				ws.setChanged(true);
				em.merge(ws);
				registry.update(ws.getKey());
				LOGGER.log(Level.FINEST, "workspace set changed");

				notification.throwEvent(object.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"));
			} else {
				LOGGER.log(Level.FINEST, "no changes detected with current object, nothing to do");
			}
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException
				| TreeBuilderException | BinaryStoreServiceException | DataNotFoundException | CloneException | IndexingServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while reading object", e);
			throw new CoreServiceException("unable to read object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveDataObject(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "moving object into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
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
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path [" + spath.build() + "] does not exists");
			}
			LOGGER.log(Level.FINEST, "source object element found for name " + spath.part());

			OrtolangObjectIdentifier soidentifier = registry.lookup(selement.getKey());
			checkObjectType(soidentifier, DataObject.OBJECT_TYPE);
			DataObject sobject = em.find(DataObject.class, soidentifier.getId());
			if (sobject == null) {
				throw new TreeBuilderException("unable to load source object with id [" + soidentifier.getId() + "] from storage");
			}
			sobject.setKey(selement.getKey());
			LOGGER.log(Level.FINEST, "source object exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			
			LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

			Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
			if (dparent.containsElementName(dpath.part())) {
				ctx.setRollbackOnly();
				throw new InvalidPathException("destination path " + dpath.build() + "already exists");
			}
			LOGGER.log(Level.FINEST, "destination element does not exists, creating it");
			if (!dpath.part().equals(spath.part())) {
				if (sobject.getClock() < ws.getClock()) {
					DataObject clone = cloneDataObject(ws.getHead(), sobject, ws.getClock());
					sobject = clone;
				}
				sobject.setName(dpath.part());
				em.merge(sobject);
				registry.update(sobject.getKey());
			}
			dparent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, sobject.getName(), System.currentTimeMillis(), sobject.getSize(), sobject.getMimeType(), sobject.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			
			LOGGER.log(Level.FINEST, "object [" + sobject.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(sobject.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "move"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while moving object", e);
			throw new CoreServiceException("unable to move object into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteDataObject(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting object into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			LOGGER.log(Level.FINEST, "object element found for path " + npath.build() + ", key: " + element.getKey());

			OrtolangObjectIdentifier oidentifier = registry.lookup(element.getKey());
			checkObjectType(oidentifier, DataObject.OBJECT_TYPE);
			DataObject leaf = em.find(DataObject.class, oidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load object with id [" + oidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			LOGGER.log(Level.FINEST, "object exists and loaded from storage");

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			
			LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}
				registry.delete(leaf.getKey());
				indexing.remove(leaf.getKey());
			}

			notification.throwEvent(leaf.getKey(), caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while deleting object", e);
			throw new CoreServiceException("unable to delete object into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	/* Link */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createLink(String workspace, String path, String target) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		String key = UUID.randomUUID().toString();
		try {
			createLink(workspace, key, path, target);
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createLink(String workspace, String key, String path, String target) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException,
			AccessDeniedException {
		LOGGER.log(Level.FINE, "create link with key [" + key + "] into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");
			authorisation.checkPermission(target, subjects, "read");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'read' permissions on the target");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

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
			indexing.index(key);

			authorisation.clonePolicy(key, ws.getHead());
			LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

			parent.addElement(new CollectionElement(Link.OBJECT_TYPE, link.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, key));
			em.merge(parent);
			registry.update(parent.getKey());
			LOGGER.log(Level.FINEST, "link [" + key + "] added to parent [" + parent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"));
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException | TreeBuilderException | IndexingServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during link creation", e);
			ctx.setRollbackOnly();
			throw new CoreServiceException("unable to create link into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "reading link with key [" + key + "]");
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

			notification.throwEvent(key, caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "read"));
			return link;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while reading link", e);
			throw new CoreServiceException("unable to read link with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void moveLink(String workspace, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "moving link into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]");
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
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
			CollectionElement selement = sparent.findElementByName(spath.part());
			if (selement == null) {
				throw new InvalidPathException("path [" + spath.build() + "] does not exists");
			}
			LOGGER.log(Level.FINEST, "source link element found for name " + spath.part());

			OrtolangObjectIdentifier slidentifier = registry.lookup(selement.getKey());
			checkObjectType(slidentifier, Link.OBJECT_TYPE);
			Link slink = em.find(Link.class, slidentifier.getId());
			if (slink == null) {
				throw new TreeBuilderException("unable to load source link with id [" + slidentifier.getId() + "] from storage");
			}
			slink.setKey(selement.getKey());
			LOGGER.log(Level.FINEST, "source link exists and loaded from storage");

			sparent.removeElement(selement);
			em.merge(sparent);
			registry.update(sparent.getKey());
			indexing.reindex(sparent.getKey());

			LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

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
				indexing.reindex(slink.getKey());

			}
			dparent.addElement(new CollectionElement(Link.OBJECT_TYPE, slink.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, slink.getKey()));
			em.merge(dparent);
			registry.update(dparent.getKey());
			indexing.reindex(dparent.getKey());

			LOGGER.log(Level.FINEST, "link [" + slink.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			notification.throwEvent(slink.getKey(), caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "move"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | CloneException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while moving link", e);
			throw new CoreServiceException("unable to move link into workspace [" + workspace + "] from path [" + source + "] to path [" + destination + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteLink(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting link into workspace [" + workspace + "] at path [" + path + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			if (npath.isRoot()) {
				throw new InvalidPathException("path is empty");
			}
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "delete");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			LOGGER.log(Level.FINEST, "link element found for name " + npath.part());

			OrtolangObjectIdentifier lidentifier = registry.lookup(element.getKey());
			checkObjectType(lidentifier, Link.OBJECT_TYPE);
			Link leaf = em.find(Link.class, lidentifier.getId());
			if (leaf == null) {
				throw new TreeBuilderException("unable to load link with id [" + lidentifier.getId() + "] from storage");
			}
			leaf.setKey(element.getKey());
			LOGGER.log(Level.FINEST, "link exists and loaded from storage");

			parent.removeElement(element);
			em.merge(parent);
			registry.update(parent.getKey());
			indexing.reindex(parent.getKey());
			LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			if (leaf.getClock() == ws.getClock()) {
				LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
				for (MetadataElement mde : leaf.getMetadatas()) {
					registry.delete(mde.getKey());
				}
				registry.delete(leaf.getKey());
				indexing.remove(leaf.getKey());
			}

			notification.throwEvent(leaf.getKey(), caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | TreeBuilderException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error while deleting link", e);
			throw new CoreServiceException("unable to delete link into workspace [" + workspace + "] at path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "finding links for target [" + target + "]");
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
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("target", target);
			notification.throwEvent("", caller, Link.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "find"), argumentsBuilder.build());
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during finding links for target", e);
			throw new CoreServiceException("unable to find link for target [" + target + "]", e);
		}
	}

	/* Metadatas */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String workspace, String path, String name, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
			MetadataFormatException {
		String key = UUID.randomUUID().toString();

		try {
			createMetadataObject(workspace, key, path, name, hash);
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createMetadataObject(String workspace, String key, String path, String name, String hash) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException,
			InvalidPathException, AccessDeniedException, MetadataFormatException {
		LOGGER.log(Level.FINE, "create metadataobject with key [" + key + "] into workspace [" + workspace + "] for path [" + path + "] with name [" + name + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			String tkey = ws.getHead();
			Collection parent = null;
			CollectionElement element = null;
			if (!npath.isRoot()) {
				parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
				LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
				element = parent.findElementByName(npath.part());
				if (element == null) {
					throw new InvalidPathException("path [" + npath.build() + "] does not exists");
				}
				LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
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

			MetadataFormat format = getMetadataFormat(name);
			if (format == null) {
				LOGGER.log(Level.SEVERE, "Unable to find a metadata format for name: " + name);
				throw new CoreServiceException("unknown metadata format for name: " + name);
			}
			validateMetadata(meta, format);
			meta.setFormat(format.getId());
			meta.setTarget(tkey);
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
						parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));
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
					parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));
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
					parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));
					em.merge(parent);
					registry.update(parent.getKey());

					link = clone;
				}
				link.addMetadata(new MetadataElement(name, key));
				em.merge(link);
				break;
			}

			registry.update(tkey);

			ws.setChanged(true);
			em.merge(ws);
			registry.update(ws.getKey());
			LOGGER.log(Level.FINEST, "workspace set changed");

			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("key", key);
			notification.throwEvent(tkey, caller, tidentifier.getType(), OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "add-metadata"), argumentsBuilder.build());
			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"));

		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
				| MembershipServiceException | TreeBuilderException | BinaryStoreServiceException | DataNotFoundException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "reading metadata for key [" + key + "]");
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
			// em.detach(meta);

			notification.throwEvent(key, caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "read"));
			return meta;
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during reading metadata", e);
			throw new CoreServiceException("unable to read metadata with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateMetadataObject(String workspace, String path, String name, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
			MetadataFormatException {
		LOGGER.log(Level.FINE, "updating metadata content into workspace [" + workspace + "] for path [" + path + "] and name [" + name + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "update");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

			String current = resolveWorkspacePath(workspace, Workspace.HEAD, npath.build());
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
			if (!cmeta.equals(hash)) {
				String tkey = ws.getHead();
				Collection parent = null;
				CollectionElement element = null;
				if (!npath.isRoot()) {
					parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
					LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
					element = parent.findElementByName(npath.part());
					if (element == null) {
						throw new InvalidPathException("path [" + npath.build() + "] does not exists");
					}
					LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
					tkey = element.getKey();
				}

				OrtolangObjectIdentifier tidentifier = registry.lookup(tkey);
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
						if (parent != null && element != null) {
							parent.removeElement(element);
							parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));

						}
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
						parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));

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
						parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));

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
					MetadataFormat format = findMetadataFormatById(meta.getFormat());
					if (format == null) {
						LOGGER.log(Level.SEVERE, "Unable to find a metadata format for name: " + name);
						throw new CoreServiceException("unknown metadata format for name: " + name);
					}
					validateMetadata(meta, format);
					meta.setTarget(tkey);
				} else {
					throw new CoreServiceException("unable to update a metadata with an empty content (hash is null)");
				}
				em.merge(meta);

				registry.update(mdelement.getKey());

				ws.setChanged(true);
				em.merge(ws);
				registry.update(ws.getKey());
				LOGGER.log(Level.FINEST, "workspace set changed");

				ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("key", mdelement.getKey());
				notification.throwEvent(mdelement.getKey(), caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "update"));
				notification.throwEvent(tkey, caller, tidentifier.getType(), OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "update-metadata"), argumentsBuilder.build());
			} else {
				LOGGER.log(Level.FINEST, "no changes detected with current metadata object, nothing to do");
			}
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException
				| TreeBuilderException | BinaryStoreServiceException | DataNotFoundException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "] and name [" + name + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteMetadataObject(String workspace, String path, String name) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting metadataobject into workspace [" + workspace + "] for path [" + path + "] with name [" + name + "]");
		try {
			PathBuilder npath = PathBuilder.fromPath(path);
			PathBuilder ppath = npath.clone().parent();

			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

			OrtolangObjectIdentifier wsidentifier = registry.lookup(workspace);
			checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
			LOGGER.log(Level.FINEST, "workspace with key [" + workspace + "] exists");

			Workspace ws = em.find(Workspace.class, wsidentifier.getId());
			if (ws == null) {
				throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
			}
			ws.setKey(workspace);
			LOGGER.log(Level.FINEST, "workspace loaded");

			authorisation.checkPermission(ws.getHead(), subjects, "create");
			LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

			Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
			LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

			CollectionElement element = parent.findElementByName(npath.part());
			if (element == null) {
				throw new InvalidPathException("path [" + npath.build() + "] does not exists");
			}
			LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
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
					parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));

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
					parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));

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
					parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));

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

			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("key", mdelement.getKey());
			notification.throwEvent(mdelement.getKey(), caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "delete"));
			notification.throwEvent(element.getKey(), caller, tidentifier.getType(), OrtolangEvent.buildEventType(tidentifier.getService(), tidentifier.getType(), "remove-metadata"), argumentsBuilder.build());
		} catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException
				| TreeBuilderException | CloneException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occured during metadata creation", e);
			throw new CoreServiceException("unable to create metadata into workspace [" + workspace + "] for path [" + path + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "finding metadata for target [" + target + "]");
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
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("target", target);
			notification.throwEvent("", caller, MetadataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "find"), argumentsBuilder.build());
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during finding metadata", e);
			throw new CoreServiceException("unable to find metadata for target [" + target + "]", e);
		}
	}

	/* MetadataFormat */

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String createMetadataFormat(String name, String description, String schema, String form) throws CoreServiceException {
		LOGGER.log(Level.FINE, "creating metadataformat with name [" + name + "]");
		try {
			MetadataFormat mdf = getMetadataFormat(name);
			MetadataFormat newmdf = new MetadataFormat();
			if (mdf != null) {
				LOGGER.log(Level.FINE, "metadata format version already exists, creating new version");
				newmdf.setSerial(mdf.getSerial() + 1);
			}
			newmdf.setName(name);
			newmdf.setId(name + ":" + newmdf.getSerial());
			newmdf.setDescription(description);
			newmdf.setForm(form);
			if (schema != null && schema.length() > 0) {
				newmdf.setSize(binarystore.size(schema));
				newmdf.setMimeType(binarystore.type(schema));
				newmdf.setSchema(schema);
			} else {
				newmdf.setSize(0);
				newmdf.setMimeType("application/octet-stream");
				newmdf.setSchema("");
			}
			em.persist(newmdf);
			return newmdf.getId();
		} catch ( BinaryStoreServiceException | DataNotFoundException e ) {
			LOGGER.log(Level.SEVERE, "unexpected error during create metadata format", e);
			throw new CoreServiceException("unexpected error during create metadata format", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<MetadataFormat> listMetadataFormat() throws CoreServiceException {
		List<MetadataFormat> formats = listAllMetadataFormat();
		HashMap<String, MetadataFormat> latest = new HashMap<String, MetadataFormat> ();
		for (MetadataFormat format : formats) {
			if ( !latest.containsKey(format.getName()) ) {
				latest.put(format.getName(), format);
			} else if ( latest.get(format.getName()).getSerial() < format.getSerial() ) {
				latest.put(format.getName(), format);
			}
		}
		List<MetadataFormat> filteredformats = new ArrayList<MetadataFormat> ();
		filteredformats.addAll(latest.values());
		return filteredformats;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<MetadataFormat> listAllMetadataFormat() throws CoreServiceException {
		TypedQuery<MetadataFormat> query = em.createNamedQuery("listMetadataFormat", MetadataFormat.class);
		List<MetadataFormat> formats = query.getResultList();
		return formats;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public MetadataFormat getMetadataFormat(String name) throws CoreServiceException {
		LOGGER.log(Level.FINE, "reading metadata format for name [" + name + "]");
		TypedQuery<MetadataFormat> query = em.createNamedQuery("findMetadataFormatForName", MetadataFormat.class).setParameter("name", name);
		List<MetadataFormat> formats = query.getResultList();
		if (formats.size() > 0) {
			return formats.get(0);
		} else {
			return null;
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public MetadataFormat findMetadataFormatById(String id) throws CoreServiceException {
		MetadataFormat format = em.find(MetadataFormat.class, id);
		if ( format == null ) {
			throw new CoreServiceException("unable to find a metadata format for id " + id + " in the storage");
		}
		return format;
	}

	private void validateMetadata(MetadataObject metadata, MetadataFormat format) throws CoreServiceException, MetadataFormatException {
		try {
			if (format.getSchema() != null && format.getSchema().length() > 0) {
				JsonNode jsonSchema = JsonLoader.fromReader(new InputStreamReader(binarystore.get(format.getSchema())));
				JsonNode jsonFile = JsonLoader.fromReader(new InputStreamReader(binarystore.get(metadata.getStream())));
				JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
				JsonSchema schema = factory.getJsonSchema(jsonSchema);

				ProcessingReport report = schema.validate(jsonFile);
				LOGGER.log(Level.INFO, report.toString());

				if (!report.isSuccess()) {
					throw new MetadataFormatException("invalid metadata format");
				}
			} else {
				LOGGER.log(Level.SEVERE, "unexpected error occured during validating metadata [" + metadata + "] with metadata format [" + format + "] : schema not found");
				throw new CoreServiceException("unable to validate metadata [" + metadata + "] with metadata format [" + format + "] : schema not found");
			}
		} catch (IOException | ProcessingException | DataNotFoundException | BinaryStoreServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during validating metadata [" + metadata + "] with metadata format [" + format + "] : schema not found");
			throw new CoreServiceException("unable to validate metadata [" + metadata + "] with metadata format [" + format + "] : schema not found");
		}
	}

	/* BinaryContent */

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public String put(InputStream data) throws CoreServiceException, DataCollisionException {
		LOGGER.log(Level.FINE, "putting binary content in store");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			String hash = binarystore.put(data);
			notification.throwEvent(hash, caller, "binary-content", OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, "binary-content", "put"));
			return hash;
		} catch (NotificationServiceException | BinaryStoreServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during putting binary content", e);
			throw new CoreServiceException("unable to put binary content", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream preview(String key) throws CoreServiceException, DataNotFoundException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "preview content from store for object with key [" + key + "]");
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
				ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("hash", hash);
				notification.throwEvent(key, caller, DataObject.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "preview"));
				return stream;
			} else {
				throw new DataNotFoundException("there is no preview available for this data object");
			}
		} catch (NotificationServiceException | BinaryStoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during getting preview content", e);
			throw new CoreServiceException("unable to get preview content", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream download(String key) throws CoreServiceException, DataNotFoundException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "download content from store for object with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			LOGGER.log(Level.INFO, "searching key " + key);
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			LOGGER.log(Level.INFO, "found key " + key);
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
				ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("hash", hash);
				notification.throwEvent(key, caller, identifier.getType(), OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, identifier.getType(), "download"), argumentsBuilder.build());
				return stream;
			} else {
				throw new DataNotFoundException("there is no preview available for this data object");
			}
		} catch (NotificationServiceException | BinaryStoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured during getting preview content", e);
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
	public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}
			IndexablePlainTextContent content = new IndexablePlainTextContent();

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
				if (object.getMimeType() != null) {
					content.addContentPart(object.getMimeType());
				}

				for (MetadataElement mde : object.getMetadatas()) {
					OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
					MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
					if (metadata == null) {
						throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
					}
					try {
						if (metadata.getStream() != null && metadata.getStream().length() > 0) {
							content.addContentPart(binarystore.extract(metadata.getStream()));
						}
					} catch (DataNotFoundException | BinaryStoreServiceException e) {
						LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
					}
				}
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(collection.getName());
				content.addContentPart(collection.getDescription());

				for (MetadataElement mde : collection.getMetadatas()) {
					OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
					MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
					if (metadata == null) {
						throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
					}
					try {
						if (metadata.getStream() != null && metadata.getStream().length() > 0) {
							content.addContentPart(binarystore.extract(metadata.getStream()));
						}
					} catch (DataNotFoundException | BinaryStoreServiceException e) {
						LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
					}
				}
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				Link reference = em.find(Link.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(reference.getName());

				for (MetadataElement mde : reference.getMetadatas()) {
					OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
					MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
					if (metadata == null) {
						throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
					}
					try {
						if (metadata.getStream() != null && metadata.getStream().length() > 0) {
							content.addContentPart(binarystore.extract(metadata.getStream()));
						}
					} catch (DataNotFoundException | BinaryStoreServiceException e) {
						LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
					}
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public IndexableSemanticContent getIndexableSemanticContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}
			IndexableSemanticContent content = new IndexableSemanticContent();

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", object.getName()));
				for (MetadataElement me : object.getMetadatas()) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#hasMetadata", URIHelper.fromKey(me.getKey())));
				}
			}

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", collection.getName()));
				for (MetadataElement me : collection.getMetadatas()) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#hasMetadata", URIHelper.fromKey(me.getKey())));
				}
			}

			if (identifier.getType().equals(Link.OBJECT_TYPE)) {
				Link reference = em.find(Link.class, identifier.getId());
				if (reference == null) {
					throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
				}
				content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.ortolang.fr/2014/05/diffusion#name", reference.getName()));
				for (MetadataElement me : reference.getMetadatas()) {
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
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException | TripleStoreServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}
			IndexableJsonContent content = new IndexableJsonContent();

			if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
				Collection collection = em.find(Collection.class, identifier.getId());
				if (collection == null) {
					throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}

				for (MetadataElement mde : collection.getMetadatas()) {
					OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
					MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
					if (metadata == null) {
						throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
					}
					try {
						if (metadata.getStream() != null && metadata.getStream().length() > 0) {
							content.put(metadata.getName(), binarystore.get(metadata.getStream()));
						}
					} catch (DataNotFoundException | BinaryStoreServiceException e) {
						LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
					}
				}
			}

			if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
				}

				for (MetadataElement mde : object.getMetadatas()) {
					OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
					MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
					if (metadata == null) {
						throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
					}
					try {
						if (metadata.getStream() != null && metadata.getStream().length() > 0) {
							content.put(metadata.getName(), binarystore.get(metadata.getStream()));
						}
					} catch (DataNotFoundException | BinaryStoreServiceException e) {
						LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
					}
				}
			}

			return content;
		} catch (RegistryServiceException | KeyNotFoundException e) {
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

	// System operations
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Set<String> systemListWorkspaceKeys(String wskey) throws CoreServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "listing workspace keys [" + wskey + "]");
		Set<String> keys = new HashSet<String>();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			checkObjectType(identifier, Workspace.OBJECT_TYPE);
			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
				throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
			}
			keys = systemListCollectionKeys(workspace.getHead(), keys);
			for (SnapshotElement snapshot : workspace.getSnapshots()) {
				keys = systemListCollectionKeys(snapshot.getKey(), keys);
			}

			notification.throwEvent(wskey, caller, identifier.getType(), OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, identifier.getType(), "system-list-keys"));
			return keys;
		} catch (NotificationServiceException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while listing workspace keys", e);
			throw new CoreServiceException("unable to list keys for workspace with key [" + wskey + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private Set<String> systemListCollectionKeys(String key, Set<String> keys) throws RegistryServiceException, KeyNotFoundException, CoreServiceException {
		OrtolangObjectIdentifier cidentifier = registry.lookup(key);
		checkObjectType(cidentifier, Collection.OBJECT_TYPE);
		Collection collection = em.find(Collection.class, cidentifier.getId());
		if (collection == null) {
			throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
		}
		if (keys.add(key)) {
			for (MetadataElement element : collection.getMetadatas()) {
				keys.add(element.getKey());
			}

			for (CollectionElement element : collection.getElements()) {
				keys.add(element.getKey());
				if (element.getType().equals(Collection.OBJECT_TYPE)) {
					systemListCollectionKeys(element.getKey(), keys);
				}
				if (element.getType().equals(DataObject.OBJECT_TYPE)) {
					OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
					checkObjectType(identifier, DataObject.OBJECT_TYPE);
					DataObject object = em.find(DataObject.class, identifier.getId());
					if (object == null) {
						throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
					}
					keys.add(element.getKey());
					for (MetadataElement mde : object.getMetadatas()) {
						keys.add(mde.getKey());
					}
				}
				if (element.getType().equals(Link.OBJECT_TYPE)) {
					OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
					checkObjectType(identifier, Link.OBJECT_TYPE);
					Link link = em.find(Link.class, identifier.getId());
					if (link == null) {
						throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
					}
					keys.add(element.getKey());
					for (MetadataElement mde : link.getMetadatas()) {
						keys.add(mde.getKey());
					}
				}
			}
		}

		return keys;
	}

	/* ### Internal operations ### */

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Collection readCollectionAtPath(String root, PathBuilder path) throws TreeBuilderException {
		LOGGER.log(Level.FINE, "Reading tree from root [" + root + "] to path [" + path.build() + "]");
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
					LOGGER.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
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
		LOGGER.log(Level.FINE, "Loading tree from root [" + root + "] to path [" + path.build() + "] with clock [" + clock + "]");
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
				LOGGER.log(Level.SEVERE, "WRONG CLOCK found for root collection key [ " + root + "]");
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
					LOGGER.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
					throw new TreeBuilderException("Internal Problem : collection [" + parts[i] + "] is a root collection but is not a root node");
				}
				if (leaf.getClock() < clock) {
					Collection clone = cloneCollection(root, leaf, clock);
					parent.removeElement(element);
					CollectionElement celement = new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey());
					parent.addElement(celement);
					registry.update(parent.getKey());
					leaf = clone;
				}
			}

			return leaf;
		} catch (KeyLockedException | InvalidPathException | RegistryServiceException | KeyNotFoundException | CoreServiceException | CloneException e) {
			throw new TreeBuilderException(e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Collection cloneCollection(String root, Collection origin, int clock) throws CloneException {
		LOGGER.log(Level.FINE, "cloning collection for origin [" + origin.getKey() + "]");
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
			indexing.index(key);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | KeyNotFoundException | AuthorisationServiceException | IndexingServiceException e) {
			throw new CloneException("unable to clone collection with origin key [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private DataObject cloneDataObject(String root, DataObject origin, int clock) throws CloneException {
		LOGGER.log(Level.FINE, "cloning object for origin [" + origin.getKey() + "]");
		try {
			String key = UUID.randomUUID().toString();

			DataObject clone = new DataObject();
			clone.setId(UUID.randomUUID().toString());
			clone.setName(origin.getName());
			clone.setDescription(origin.getDescription());
			clone.setSize(origin.getSize());
			clone.setMimeType(origin.getMimeType());
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
			indexing.index(key);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException | IndexingServiceException e) {
			throw new CloneException("unable to clone object with origin [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private Link cloneLink(String root, Link origin, int clock) throws CloneException {
		LOGGER.log(Level.FINE, "cloning link for origin [" + origin + "]");
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
			indexing.index(key);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException | IndexingServiceException e) {
			throw new CloneException("unable to clone link with origin [" + origin + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private MetadataObject cloneMetadataObject(String root, String origin, String target) throws CloneException {
		LOGGER.log(Level.FINE, "clone metadata for origin [" + origin + "], target [" + target + "]");
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
			clone.setFormat(meta.getFormat());
			clone.setContentType(meta.getContentType());
			clone.setStream(meta.getStream());
			clone.setKey(key);
			em.persist(clone);

			registry.register(key, clone.getObjectIdentifier(), origin, true);
			indexing.reindex(target);
			authorisation.clonePolicy(key, root);

			return clone;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | CoreServiceException | KeyNotFoundException | KeyAlreadyExistsException | IndexingServiceException e) {
			throw new CloneException("unable to clone metadata with origin [" + origin + "] and target [" + target + "]", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private OrtolangObjectSize getCollectionSize(String key, OrtolangObjectIdentifier cidentifier, OrtolangObjectSize ortolangObjectSize, List<String> subjects) throws KeyNotFoundException,
			RegistryServiceException, CoreServiceException {
		LOGGER.log(Level.FINE, "calculating collection size for collection with key [" + key + "]");
		try {
			authorisation.checkPermission(key, subjects, "read");

			Collection collection = em.find(Collection.class, cidentifier.getId());
			if (collection == null) {
				throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
			}

			for (CollectionElement element : collection.getElements()) {
				if (element.getType().equals(DataObject.OBJECT_TYPE)) {
					try {
						authorisation.checkPermission(element.getKey(), subjects, "read");
						ortolangObjectSize.addElement(element.getType(), element.getSize());
					} catch (AuthorisationServiceException | AccessDeniedException e) {
						ortolangObjectSize.setPartial(true);
					}
				} else if (element.getType().equals(Collection.OBJECT_TYPE)) {
					ortolangObjectSize.addElement(element.getType(), 0);
					OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
					ortolangObjectSize = getCollectionSize(element.getKey(), identifier, ortolangObjectSize, subjects);
				}
			}
		} catch (AuthorisationServiceException | AccessDeniedException e) {
			ortolangObjectSize.setPartial(true);
		}
		return ortolangObjectSize;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void deleteCollectionContent(Collection collection, int clock) throws CoreServiceException, RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "delete content for collection with id [" + collection.getId() + "]");
		for (CollectionElement element : collection.getElements()) {
			if (element.getType().equals(Collection.OBJECT_TYPE)) {
				OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
				checkObjectType(identifier, Collection.OBJECT_TYPE);
				Collection coll = em.find(Collection.class, identifier.getId());
				if (coll == null) {
					throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
				}
				deleteCollectionContent(coll, clock);
			}
			if (element.getType().equals(DataObject.OBJECT_TYPE)) {
				OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
				checkObjectType(identifier, DataObject.OBJECT_TYPE);
				DataObject object = em.find(DataObject.class, identifier.getId());
				if (object == null) {
					throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
				}
				if (object.getClock() == clock) {
					LOGGER.log(Level.FINEST, "object clock [" + object.getClock() + "] is the same, key can be deleted and unindexed");
					for (MetadataElement mde : object.getMetadatas()) {
						registry.delete(mde.getKey());
					}
					registry.delete(element.getKey());

				}
			}
			if (element.getType().equals(Link.OBJECT_TYPE)) {
				OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
				checkObjectType(identifier, Link.OBJECT_TYPE);
				Link link = em.find(Link.class, identifier.getId());
				if (link == null) {
					throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
				}
				if (link.getClock() == clock) {
					LOGGER.log(Level.FINEST, "link clock [" + link.getClock() + "] is the same, key can be deleted and unindexed");
					for (MetadataElement mde : link.getMetadatas()) {
						registry.delete(mde.getKey());
					}
					registry.delete(element.getKey());

				}
			}
		}
	}

}
