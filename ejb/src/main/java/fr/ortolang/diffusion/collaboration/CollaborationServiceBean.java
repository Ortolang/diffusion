package fr.ortolang.diffusion.collaboration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.collaboration.entity.Project;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.CollectionProperty;
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

@Remote(CollaborationService.class)
@Local(CollaborationServiceLocal.class)
@Stateless(name = CollaborationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class CollaborationServiceBean implements CollaborationService, CollaborationServiceLocal {

	private Logger logger = Logger.getLogger(CollaborationServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private CoreService core;
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

	public CollaborationServiceBean() {
	}
	
	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registry = registryService;
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
	public void createProject(String key, String name, String type) throws CollaborationServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating new project for key [" + key + "]");
		String id = UUID.randomUUID().toString();
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			String members = UUID.randomUUID().toString();
			membership.createGroup(members, "members", "members of project '" + name + "'");
			membership.addMemberInGroup(members, caller);

			String root = UUID.randomUUID().toString();
			core.createCollection(root, "/", "root collection for workspace " + name);
			registry.setProperty(root, CollectionProperty.LEVEL, CollectionProperty.Level.TOP.name());
			registry.setProperty(root, CollectionProperty.VERSION, CollectionProperty.Version.WORK.name());
			Map<String, List<String>> rules = new HashMap<String, List<String>>();
			rules.put(members, Arrays.asList("read", "update", "delete"));
			authorisation.setPolicyRules(root, rules);

			Project project = new Project();
			project.setId(id);
			project.setName(name);
			project.setType(type);
			project.setRoot(root);
			project.setMembers(members);
			em.persist(project);

			registry.register(key, project.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);

			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			logger.log(Level.FINE, "the key [" + key + "] is already used");
			ctx.setRollbackOnly();
			throw e;
		} catch (CoreServiceException | IndexingServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException
				| IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during project creation", e);
			ctx.setRollbackOnly();
			throw new CollaborationServiceException("unable to create project with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Project readProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading project for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Project.OBJECT_TYPE);
			Project project = em.find(Project.class, identifier.getId());
			if (project == null) {
				logger.log(Level.SEVERE, "project with id [" + identifier.getId() + "] does not exists in the storage");
				throw new CollaborationServiceException("unable to load project with id [" + identifier.getId() + "] from storage");
			}
			project.setKey(key);

			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "read"), "");
			return project;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during project reading", e);
			throw new CollaborationServiceException("unable to read project with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findMyProjects() throws CollaborationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding project for connected profile");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(caller);
			
			List<String> keys = new ArrayList<String>();
			TypedQuery<Project> query = em.createNamedQuery("findProjectByMember", Project.class).setParameter("groups", subjects);
			List<Project> projects = query.getResultList();
			for ( Project project : projects ) {
				OrtolangObjectIdentifier identifier = project.getObjectIdentifier(); 
				try {
					keys.add(registry.lookup(identifier));
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.WARNING, "a project with an unregistered identifier has be found : " + identifier);
				}
			}

			notification.throwEvent("", caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "find"), "");
			return keys;
		} catch (NotificationServiceException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
			logger.log(Level.SEVERE, "unexpected error occured during finding projects for connected profile", e);
			throw new CollaborationServiceException("unable to find projects for connected profile", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateProject(String key, String name) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating project for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Project.OBJECT_TYPE);
			Project project = em.find(Project.class, identifier.getId());
			if (project == null) {
				logger.log(Level.SEVERE, "project object with id [" + identifier.getId() + "] does not exists in the storage");
				throw new CollaborationServiceException("unable to load project with id [" + identifier.getId() + "] from storage");
			}
			project.setName(name);
			em.merge(project);

			registry.setProperty(project.getRoot(), OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "update"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured during workspace update", e);
			ctx.setRollbackOnly();
			throw new CollaborationServiceException("unable to update workspace with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting project for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Project.OBJECT_TYPE);

			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "delete"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new CollaborationServiceException("unable to delete project with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void snapshotProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "create snapshot of project with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "snapshot");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Project.OBJECT_TYPE);
			Project project = em.find(Project.class, identifier.getId());
			if (project == null) {
				logger.log(Level.SEVERE, "project object with id [" + identifier.getId() + "] does not exists in the storage");
				throw new CollaborationServiceException("unable to load project with id [" + identifier.getId() + "] from storage");
			}
			String root = project.getRoot();
			String newroot = UUID.randomUUID().toString();
			// TODO optimize snapshot operation in order to avoid unnecessary clones (between 2 snapshots)
			core.cloneCollectionContent(newroot, root);
			registry.setProperty(root, CollectionProperty.VERSION, CollectionProperty.Version.SNAPSHOT.name() + "." + System.currentTimeMillis());
			
			project.setRoot(newroot);
			project.addVersion(root);
			em.merge(project);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "snapshot"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new CollaborationServiceException("unable to create snapshot for project with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void releaseProject(String key, String name) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "release project with key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "release");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Project.OBJECT_TYPE);
			Project project = em.find(Project.class, identifier.getId());
			if (project == null) {
				logger.log(Level.SEVERE, "project object with id [" + identifier.getId() + "] does not exists in the storage");
				throw new CollaborationServiceException("unable to load project with id [" + identifier.getId() + "] from storage");
			}
			String root = project.getRoot();
			String newroot = UUID.randomUUID().toString();
			core.cloneCollectionContent(newroot, root);
			registry.setProperty(root, CollectionProperty.VERSION, CollectionProperty.Version.RELEASE.name() + "." + name);
			
			project.setRoot(newroot);
			project.addVersion(root);
			em.merge(project);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			
			indexing.reindex(key);
			
			notification.throwEvent(key, caller, Project.OBJECT_TYPE, OrtolangEvent.buildEventType(CollaborationService.SERVICE_NAME, Project.OBJECT_TYPE, "release"), "name=" + name);
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new CollaborationServiceException("unable to create snapshot for project with key [" + key + "]", e);
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

			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Project.OBJECT_TYPE)) {
				return readProject(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (KeyNotFoundException | CollaborationServiceException | RegistryServiceException | AccessDeniedException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("system")
	public OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableContent content = new OrtolangIndexableContent();

			if (identifier.getType().equals(Project.OBJECT_TYPE)) {
				Project project = em.find(Project.class, identifier.getId());
				if (project == null) {
					throw new OrtolangException("unable to find project with id [" + identifier.getId() + "] from storage");
				}
				content.addContentPart(project.getName());
				content.addContentPart(project.getType());
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws CollaborationServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new CollaborationServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new CollaborationServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}