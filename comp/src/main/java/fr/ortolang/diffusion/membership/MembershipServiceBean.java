package fr.ortolang.diffusion.membership;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableJsonContent;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authentication.AuthenticationService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.triple.Triple;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;
import fr.ortolang.diffusion.store.triple.URIHelper;

@Local(MembershipService.class)
@Stateless(name = MembershipService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class MembershipServiceBean implements MembershipService {

	private Logger logger = Logger.getLogger(MembershipServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
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
	@PermitAll
	public String getProfileKeyForIdentifier(String identifier) {
		return identifier;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException {
		logger.log(Level.FINE, "getting connected identifier subjects");
		try {
			String caller = getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(caller);
			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}

			String[] groups = profile.getGroups();
			List<String> subjects = new ArrayList<String>(groups.length + 2);
			subjects.add(caller);
			if ( profile.getStatus().equals(ProfileStatus.ACTIVE) ) {
				if ( !caller.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
					subjects.add(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY);
				}
				subjects.addAll(Arrays.asList(groups));
			}

			return subjects;
		} catch (RegistryServiceException e) {
			throw new MembershipServiceException("unable to get connected identifier subjects", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Profile createProfile(String givenName, String familyName, String email) throws MembershipServiceException, ProfileAlreadyExistsException {
		logger.log(Level.FINE, "creating profile for connected identifier");

		String connectedIdentifier = authentication.getConnectedIdentifier();
		String key = getProfileKeyForConnectedIdentifier();

		try {
			try {
				registry.lookup(key);
				throw new ProfileAlreadyExistsException("A profile already exists for identifier: " + connectedIdentifier);
			} catch ( KeyNotFoundException e ) {
			}
			
			Profile profile = new Profile();
			profile.setId(connectedIdentifier);
			profile.setGivenName(givenName);
			profile.setFamilyName(familyName);
			profile.setEmail(email);
			profile.setFriends(connectedIdentifier+"-friends");
			profile.setStatus(ProfileStatus.ACTIVE);
			em.persist(profile);

			registry.register(key, profile.getObjectIdentifier(), key);

			authorisation.createPolicy(key, key);
			Map<String, List<String>> readRules = new HashMap<String, List<String>>();
			readRules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList(new String[] { "read" }));
			authorisation.setPolicyRules(key, readRules);

			notification.throwEvent(key, key, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "create"), "");
			return profile;
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException | KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create profile with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createProfile(String identifier, String givenName, String familyName, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating profile for identifier [" + identifier + "] and email [" + email + "]");

		String key = getProfileKeyForIdentifier(identifier);
		logger.log(Level.FINEST, "generated profile key [" + key + "]");

		try {
			String caller = getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);

			Profile profile = new Profile();
			profile.setId(identifier);
			profile.setGivenName(givenName);
			profile.setFamilyName(familyName);
			profile.setEmail(email);
			profile.setFriends(identifier+"-friends");
			profile.setStatus(ProfileStatus.ACTIVE);
			em.persist(profile);

			registry.register(key, profile.getObjectIdentifier(), caller);
			
			authorisation.createPolicy(key, key);
			Map<String, List<String>> readRules = new HashMap<String, List<String>>();
			readRules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList(new String[] { "read" }));
			authorisation.setPolicyRules(key, readRules);

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "create"), "");
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw new ProfileAlreadyExistsException("a profile already exists for identifier " + identifier, e);
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create profile with key [" + key + "]", e);
		}
	}
	

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	//TODO refactor
	public List<Profile> listProfiles() throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "listing profiles");
		try {
			List<Profile> ProfilesList = new ArrayList<Profile>();
			List<String> listKeys = registry.list(1, 100, OrtolangObjectIdentifier.buildJPQLFilterPattern(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE), null, false);
			for (String key : listKeys) {
				List<String> subjects = getConnectedIdentifierSubjects();
				authorisation.checkPermission(key, subjects, "read");
	
				OrtolangObjectIdentifier identifier = registry.lookup(key);
				checkObjectType(identifier, Profile.OBJECT_TYPE);
				Profile profile = em.find(Profile.class, identifier.getId());
				if (profile == null) {
					throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
				}
				ProfilesList.add(profile);
			}
			return ProfilesList;
			
		} catch (AuthorisationServiceException | RegistryServiceException e) {
			throw new MembershipServiceException("unable to list the profiles", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	//TODO refactor
	public List<Profile> searchProfile(String data) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "searching profiles with " + data);
		try {
			List<Profile> ProfilesList = new ArrayList<Profile>();
			List<String> listKeys = registry.list(1, 100, OrtolangObjectIdentifier.buildJPQLFilterPattern(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE), null, false);
			for (String key : listKeys) {
				List<String> subjects = getConnectedIdentifierSubjects();
				authorisation.checkPermission(key, subjects, "read");
	
				OrtolangObjectIdentifier identifier = registry.lookup(key);
				checkObjectType(identifier, Profile.OBJECT_TYPE);
				Profile profile = em.find(Profile.class, identifier.getId());
				if (profile == null) {
					throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
				}
				String id = profile.getId();
				String name = profile.getFullName();
				if(id.matches("(?i).*" + data + ".*") || name.matches("(?i).*" + data + ".*")){
					ProfilesList.add(profile);
				}
			}
			return ProfilesList;
			
		} catch (AuthorisationServiceException | RegistryServiceException e) {
			throw new MembershipServiceException("unable to list the profiles", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading profile for key [" + key + "]");
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
	public List<ProfileData> listProfileInfos(String key, String filter) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "list profile infos for profile with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			Profile profile = em.find(Profile.class, key);
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			
			ProfileDataVisibility visibilityLevel = ProfileDataVisibility.EVERYBODY;
			try {
				authorisation.checkOwnership(key, subjects);
				visibilityLevel = ProfileDataVisibility.NOBODY;
			} catch(AccessDeniedException e1) {
				if(profile.getFriends()!=null) {
					String friendsGroupKey = profile.getFriends();
					OrtolangObjectIdentifier friendsObject = registry.lookup(friendsGroupKey);
					checkObjectType(friendsObject, Group.OBJECT_TYPE);
					try {
						if (isMember(friendsGroupKey, caller)){
							visibilityLevel = ProfileDataVisibility.FRIENDS;
						} 
					} catch (AccessDeniedException e2){
						logger.log(Level.FINE, caller + " is not authorized to read friend list of profile with key [" + key + "]");						
					}
				}
			}
			logger.log(Level.FINE, "Visibility level set to " + visibilityLevel);
			
			List<ProfileData> visibleInfos = new ArrayList<ProfileData> ();
			for ( ProfileData info : profile.getInfos().values() ) {
				logger.log(Level.FINE, "Traeating info " + info.getName() );
				if ( visibilityLevel.getValue() >= info.getVisibility() ) {
					logger.log(Level.FINE, "info is visible");
					if ( filter != null && filter.length() > 0 ) {
						if ( info.getName().matches(filter+"(.*)") ) {
							logger.log(Level.FINE, "info name matches filter");
							visibleInfos.add(info);
						}
					} else {
						logger.log(Level.FINE, "filter is null or empty, adding info");
						visibleInfos.add(info);
					}
				}
			}
			
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "list-infos"), "");
			return visibleInfos;
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to listy profile infos for profile with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setProfileInfo(String key, String name, String value, int visibility, ProfileDataType type, String source) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "set profile info [" +  name + "] for profile with key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			Profile profile = em.find(Profile.class, key);
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			
			ProfileData info = null;
			if ( value != null && value.length() > 0 ) {
				info = new ProfileData(name, value, visibility, type, source);
			}
			profile.setInfo(name, info);
			em.merge(profile);
						
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "set-info"), "name=" + name);
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException e) {
			throw new MembershipServiceException("unable to set profile info for profile with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateProfile(String key, String givenName, String familyName, String email) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating profile for key [" + key + "]");
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
			profile.setGivenName(givenName);
			profile.setFamilyName(familyName);
			profile.setEmail(email);
			em.merge(profile);

			registry.update(key);
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "update"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to update the profile with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting profile for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Profile.OBJECT_TYPE);
			Profile profile = em.find(Profile.class, identifier.getId());
			if (profile == null) {
				throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
			}
			profile.setStatus(ProfileStatus.DELETED);
			em.merge(profile);
			
			registry.update(key);
			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "delete"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to delete object with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void addProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "adding public key to profile with key [" + key + "]");
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
			profile.addPublicKey(pubkey);
			em.merge(profile);

			registry.update(key);

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "add-key"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to add public key to profile with key [" + key + "]");
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "removing public key to profile with key [" + key + "]");
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
			profile.removePublicKey(pubkey);
			em.merge(profile);

			registry.update(key);

			notification.throwEvent(key, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "remove-key"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to remove public key to profile with key [" + key + "]");
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating group for key [" + key + "] and name [" + name + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);

			if ( key.equals(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY ) ) {
				throw new MembershipServiceException("key [" + key + "] is reserved for all authentified users and cannot be used for a group");
			}
			
			Group group = new Group();
			group.setId(UUID.randomUUID().toString());
			group.setName(name);
			group.setDescription(description);
			em.persist(group);

			registry.register(key, group.getObjectIdentifier(), caller);
			
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "create"), "");
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyNotFoundException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to create group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "reading group for key [" + key + "]");
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
		logger.log(Level.FINE, "updating group for key [" + key + "]");
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

			registry.update(key);

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "update"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("error while trying to update the group with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting group for key [" + key + "]");
		try {
			String caller = getProfileKeyForConnectedIdentifier();
			List<String> subjects = getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier gidentifier = registry.lookup(key);
			checkObjectType(gidentifier, Group.OBJECT_TYPE);
			Group group = em.find(Group.class, gidentifier.getId());
			if (group == null) {
				throw new MembershipServiceException("unable to find a group for id " + gidentifier.getId());
			}
			for ( String pkey : group.getMembers() ) {
				OrtolangObjectIdentifier pidentifier = registry.lookup(pkey);
				checkObjectType(pidentifier, Profile.OBJECT_TYPE);
				Profile profile = em.find(Profile.class, pidentifier.getId());
				if (profile == null) {
					throw new MembershipServiceException("unable to find a profile for id " + pidentifier.getId());
				}
				profile.removeGroup(key);
				em.merge(profile);
				try {
					registry.update(pkey);
				} catch ( Exception e ) {
					logger.log(Level.WARNING, "unable to update profile key [" + pkey + "]", e);
				}
			}
			
			registry.delete(key);
			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "delete"), "");
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to delete group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "adding member in group for key [" + key + "] and member [" + member + "]");
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

			registry.update(key);
			registry.update(member);

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "add-member"), "member="
					+ member);
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to add member in group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "removing member [" + member + "] from group with key [" + key + "]");
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

			registry.update(key);
			registry.update(member);

			notification.throwEvent(key, caller, Group.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "remove-member"), "member="
					+ member);
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to remove member from group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "joining group with key [" + key + "]");
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

			registry.update(key);
			registry.update(caller);

			notification.throwEvent(caller, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "join"), "group=" + key);
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to join group with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "leaving group with key [" + key + "]");
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

			registry.update(key);
			registry.update(caller);

			notification
					.throwEvent(caller, caller, Profile.OBJECT_TYPE, OrtolangEvent.buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "leave"), "group=" + key);
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new MembershipServiceException("unable to leave group with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "listing members of group with key [" + key + "]");
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
		logger.log(Level.FINE, "listing groups of profile with key [" + key + "]");
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
		logger.log(Level.FINE, "checking membership of member [" + member + "] in group with key [" + key + "]");
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
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
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
		} catch (MembershipServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
        logger.log(Level.FINE, "calculating size for object with key [" + key + "]");
        try {
            List<String> subjects = getConnectedIdentifierSubjects();
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            OrtolangObjectSize ortolangObjectSize = new OrtolangObjectSize();
            authorisation.checkPermission(key, subjects, "read");
            switch (identifier.getType()) {
                case Group.OBJECT_TYPE: {
                    ortolangObjectSize.addElements("member", readGroup(key).getMembers().length);
                    break;
                }
                case Profile.OBJECT_TYPE: {
                    Profile profile = readProfile(key);
                    ortolangObjectSize.addElements("groups", profile.getGroups().length);
                    ortolangObjectSize.addElements("keys", profile.getKeys().size());
                    break;
                }
            }
            return ortolangObjectSize;
        } catch (MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
            logger.log(Level.SEVERE, "unexpected error while calculating object size", e);
            throw new OrtolangException("unable to calculate size for object with key [" + key + "]", e);
        }
    }

    @Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangIndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();

			if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
				Profile profile = em.find(Profile.class, identifier.getId());
				if (profile != null) {
					content.addContentPart(profile.getFullName());
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
			throw new OrtolangException("unable to get indexable plain text content for key " + key, e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangIndexableSemanticContent getIndexableSemanticContent(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			OrtolangIndexableSemanticContent content = new OrtolangIndexableSemanticContent();
			
			if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
				Profile profile = em.find(Profile.class, identifier.getId());
				if (profile != null) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://xmlns.com/foaf/0.1/Person"));
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/givenName", profile.getGivenName()));
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/familyName", profile.getFamilyName()));
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/mbox", profile.getEmail()));
					for ( String group : profile.getGroups() ) {
						content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/member", URIHelper.fromKey(group)));
					}
				}
			}

			if (identifier.getType().equals(Group.OBJECT_TYPE)) {
				Group group = em.find(Group.class, identifier.getId());
				if (group != null) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://xmlns.com/foaf/0.1/Group"));
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException | TripleStoreServiceException e) {
			throw new OrtolangException("unable to get indexable semantic content for key " + key, e);
		} 
	}

	@Override
	public OrtolangIndexableJsonContent getIndexableJsonContent(String key)
			throws OrtolangException {
		return null;
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
