package fr.ortolang.diffusion.membership;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authentication.AuthenticationService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Remote(MembershipService.class)
@Local(MembershipServiceLocal.class)
@Stateless(name = MembershipService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class MembershipServiceBean implements MembershipService, MembershipServiceLocal {

	private Logger logger = Logger.getLogger(MembershipServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
	@EJB
	private IndexingService indexing;
	@EJB
	private AuthenticationService authentication;
	@EJB
	private AuthorisationService authorisation;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	
	public MembershipServiceBean() {
	}

	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registry) {
		this.registry = registry;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notification) {
		this.notification = notification;
	}

	public IndexingService getIndexingService() {
		return indexing;
	}

	public void setIndexingService(IndexingService indexing) {
		this.indexing = indexing;
	}

	public AuthenticationService getAuthenticationService() {
		return authentication;
	}

	public void setAuthenticationService(AuthenticationService authentication) {
		this.authentication = authentication;
	}

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getProfileKeyForConnectedIdentifier() {
		return authentication.getConnectedIdentifier();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getProfileKeyForIdentifier(String identifier) {
		return identifier;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting connected identifier subjects");
		try {
			String caller = getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(caller);
			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}

			String[] groups = profile.getGroups();
			List<String> subjects = new ArrayList<String>(groups.length + 1);
			subjects.add(caller);
			subjects.addAll(Arrays.asList(groups));

			return subjects;
		} catch (RegistryServiceException e) {
			throw new MembershipServiceException("unable to get connected identifier subjects", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createProfile(String fullname, String email) throws MembershipServiceException, ProfileAlreadyExistsException {
		logger.log(Level.INFO, "creating profile for connected identifier");

		String connectedIdentifier = authentication.getConnectedIdentifier();
		String key = getProfileKeyForConnectedIdentifier();

		try {
			
			Profile profile = new Profile();
			profile.setId(connectedIdentifier);
			profile.setFullname(fullname);
			profile.setEmail(email);
			profile.setStatus(ProfileStatus.ACTIVATED);
			em.persist(profile);

			registry.register(key, profile.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, key);
			
			authorisation.createPolicy(key, key);

			indexing.index(key);
			notification.throwEvent(key, key, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new ProfileAlreadyExistsException("a profile already exists for connected identifier: " + connectedIdentifier, e);
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | KeyNotFoundException | IndexingServiceException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create profile with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createProfile(String identifier, String fullname, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating profile for identifier [" + identifier + "] and email [" + email + "]");

		String key = getProfileKeyForIdentifier(identifier);
		logger.log(Level.FINEST, "generated profile key [" + key + "]");

		try {
			String caller = getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);

			Profile profile = new Profile();
			profile.setId(identifier);
			profile.setFullname(fullname);
			profile.setEmail(email);
			profile.setStatus(ProfileStatus.ACTIVATED);
			em.persist(profile);

			registry.register(key, profile.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			
			authorisation.createPolicy(key, key);

			indexing.index(key);
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new ProfileAlreadyExistsException("a profile already exists for identifier " + identifier, e);
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | KeyNotFoundException | IndexingServiceException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create profile with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading profile for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			profile.setKey(key);

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "read"), "");
			return profile;
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to read the profile with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateProfile(String key, String fullname) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating profile for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			profile.setFullname(fullname);
			em.merge(profile);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | IndexingServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to update the profile with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting profile for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "delete"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to delete object with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating group for key [" + key + "] and name [" + name + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			Group group = new Group();
			group.setId(UUID.randomUUID().toString());
			group.setName(name);
			group.setDescription(description);
			em.persist(group);

			registry.register(key, group.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			
			authorisation.createPolicy(key, caller);

			indexing.index(key);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "create"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | KeyNotFoundException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading group for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Group.OBJECT_TYPE);
			Group group = em.find(Group.class, identifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
			}
			group.setKey(key);

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "read"), "");
			return group;
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to read the group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateGroup(String key, String name, String description) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating group for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Group.OBJECT_TYPE);
			Group group = em.find(Group.class, identifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
			}
			group.setName(name);
			group.setDescription(description);
			em.merge(group);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | IndexingServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to update the group with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "deleting group for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Group.OBJECT_TYPE);
			registry.delete(key);
			indexing.remove(key);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "delete"), "");
		} catch (IndexingServiceException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to delete group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "adding member in group for key [" + key + "] and member [" + member + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");
			authorisation.checkPermission(member, subjects, "read");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			OrtolangObjectIdentifier midentifier = registry.lookup(member);
			checkObjectType(midentifier, Profile.OBJECT_TYPE);

			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			Profile profile = em.find(Profile.class, midentifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + midentifier.getId());
			}
			group.addMember(member);
			profile.addGroup(key);
			em.merge(profile);
			em.merge(group);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(member, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			indexing.reindex(member);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "add-member"), "member="
					+ member);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to add member in group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "removing member [" + member + "] from group with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");
			authorisation.checkPermission(member, subjects, "read");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			OrtolangObjectIdentifier midentifier = registry.lookup(member);
			checkObjectType(midentifier, Profile.OBJECT_TYPE);

			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			if (!group.isMember(member)) {
				throw new MembershipServiceException("Profile " + member + " is not member of group with key [" + key + "]");
			}
			Profile profile = em.find(Profile.class, midentifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + midentifier.getId());
			}
			group.removeMember(member);
			profile.removeGroup(key);
			em.merge(profile);
			em.merge(group);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(member, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			indexing.reindex(member);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "remove-member"), "member="
					+ member);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to remove member from group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "joining group with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			OrtolangObjectIdentifier midentifier = registry.lookup(caller);
			checkObjectType(midentifier, Profile.OBJECT_TYPE);

			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			Profile profile = em.find(Profile.class, midentifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + midentifier.getId());
			}
			group.addMember(caller);
			profile.addGroup(key);
			em.merge(profile);
			em.merge(group);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(caller, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			indexing.reindex(caller);
			notification.throwEvent(caller, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "join"), "group=" + key);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to join group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "leaving group with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			OrtolangObjectIdentifier midentifier = registry.lookup(caller);
			checkObjectType(midentifier, Profile.OBJECT_TYPE);

			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			if (!group.isMember(caller)) {
				throw new MembershipServiceException("Profile " + caller + " is not member of group with key [" + key + "]");
			}
			Profile profile = em.find(Profile.class, midentifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + midentifier.getId());
			}
			group.removeMember(caller);
			profile.removeGroup(key);
			em.merge(profile);
			em.merge(group);

			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(caller, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());

			indexing.reindex(key);
			indexing.reindex(caller);
			notification
					.throwEvent(caller, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "leave"), "group=" + key);
		} catch (NotificationServiceException | RegistryServiceException | IndexingServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to leave group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing members of group with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Group.OBJECT_TYPE);

			Group group = em.find(Group.class, identifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
			}
			String[] members = group.getMembers();

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "list-members"), "");
			return Arrays.asList(members);
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to list members in group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> getProfileGroups(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing groups of profile with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);

			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			String[] groups = profile.getGroups();

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "list-groups"), "");
			return Arrays.asList(groups);
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to list groups of profile with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isMember(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "checking membership of member [" + member + "] in group with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			authorisation.checkPermission(member, subjects, "read");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			OrtolangObjectIdentifier midentifier = registry.lookup(member);
			checkObjectType(midentifier, Profile.OBJECT_TYPE);

			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			boolean isMember = false;
			if (group.isMember(member)) {
				isMember = true;
			}

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "is-member"), "member="
					+ member);
			return isMember;
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to check membership in group with key [" + key + "]", e);
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

			if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
				return readProfile(key);
			}

			if (identifier.getType().equals(Group.OBJECT_TYPE)) {
				return readGroup(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (KeyNotFoundException | MembershipServiceException | RegistryServiceException | AccessDeniedException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@RolesAllowed("system")
	public OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableContent content = new OrtolangIndexableContent();

			if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
				Profile profile = em.find(Profile.class, identifier.getId());
				if (profile != null) {
					content.addContentPart(profile.getFullname());
					content.addContentPart(profile.getEmail());
					content.addContentPart(profile.getGroupsList());
				}
			}

			if (identifier.getType().equals(Group.OBJECT_TYPE)) {
				Group group = em.find(Group.class, identifier.getId());
				if (group != null) {
					content.addContentPart(group.getName());
					content.addContentPart(group.getDescription());
					content.addContentPart(group.getMembersList());
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws MembershipServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new MembershipServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new MembershipServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}
