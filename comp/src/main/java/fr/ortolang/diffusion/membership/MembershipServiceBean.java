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

import static fr.ortolang.diffusion.OrtolangEvent.buildEventType;
import static org.bouncycastle.crypto.tls.ConnectionEnd.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
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
import fr.ortolang.diffusion.security.authentication.TOTPHelper;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

@Local(MembershipService.class)
@Stateless(name = MembershipService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class MembershipServiceBean implements MembershipService {

    private static final Logger LOGGER = Logger.getLogger(MembershipServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { Group.OBJECT_TYPE, Profile.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { Profile.OBJECT_TYPE, "read,update,delete" }, { Group.OBJECT_TYPE, "read,update,delete" } };

    @EJB
    private RegistryService registry;
    @EJB
    private NotificationService notification;
    @EJB
    private AuthenticationService authentication;
    @EJB
    private IndexingService indexing;
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
        LOGGER.log(Level.FINE, "getting connected identifier subjects");
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
            if (profile.getStatus().equals(ProfileStatus.ACTIVE)) {
                if (!caller.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    subjects.add(MembershipService.UNAUTHENTIFIED_IDENTIFIER);
                    subjects.add(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY);
                }
                if (ctx.isCallerInRole(MembershipService.ADMINS_GROUP_KEY)) {
                    subjects.add(MembershipService.ADMINS_GROUP_KEY);
                }
                if (ctx.isCallerInRole(MembershipService.ESR_GROUP_KEY)) {
                    subjects.add(MembershipService.ESR_GROUP_KEY);
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
    public Profile createProfile(String givenName, String familyName, String email) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException {
        LOGGER.log(Level.FINE, "creating profile for connected identifier");
        String identifier = authentication.getConnectedIdentifier();
        return createProfile(identifier, givenName, familyName, email, ProfileStatus.ACTIVE);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Profile createProfile(String identifier, String givenName, String familyName, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException,
            AccessDeniedException {
        String key = getProfileKeyForIdentifier(identifier);
        LOGGER.log(Level.FINE, "creating profile for identifier [" + identifier + "], key [" + key + "] and email [" + email + "]");

        try {
            String caller = getProfileKeyForConnectedIdentifier();
            if (!caller.equals(key)) {
                // Only Super User can create any profile
                authorisation.checkSuperUser(caller);
            }

            String friendGroupKey = identifier + "-friends";

            Profile profile = new Profile();
            profile.setId(identifier);
            profile.setGivenName(givenName);
            profile.setFamilyName(familyName);
            profile.setEmail(email);
            profile.setEmailHash(hashEmail(email));
            profile.setFriends(friendGroupKey);
            profile.setStatus(status);
            em.persist(profile);

            registry.register(key, profile.getObjectIdentifier(), caller);
            if (!key.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                indexing.index(key);
            }

            authorisation.createPolicy(key, key);
            Map<String, List<String>> readRules = new HashMap<String, List<String>>();
            readRules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Collections.singletonList("read"));
            authorisation.setPolicyRules(key, readRules);

            createGroup(friendGroupKey, identifier + "'s Collaborators", "List of collaborators of user " + identifier);
            Map<String, List<String>> friendsReadRules = new HashMap<String, List<String>>();
            friendsReadRules.put(friendGroupKey, Collections.singletonList("read"));
            authorisation.setPolicyRules(friendGroupKey, friendsReadRules);
            if (!caller.equals(key)) {
                authorisation.updatePolicyOwner(friendGroupKey, key);
            }

            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "create"));
            return profile;
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            throw new ProfileAlreadyExistsException("a profile already exists for identifier " + identifier, e);
        } catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException | NoSuchAlgorithmException
                | UnsupportedEncodingException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to create profile with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading profile for key [" + key + "]");
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
            if (profile.getEmailVisibility() != ProfileDataVisibility.EVERYBODY) {
                ProfileDataVisibility visibilityLevel = getVisibilityLevel(caller, key, subjects, profile);
                if (visibilityLevel.compareTo(profile.getEmailVisibility()) < 0) {
                    profile.setEmail(null);
                }
            }

            return profile;
        } catch (RegistryServiceException | AuthorisationServiceException e) {
            throw new MembershipServiceException("unable to read the profile with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<ProfileData> listProfileInfos(String key, String filter) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "list profile infos for profile with key [" + key + "]");
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

            ProfileDataVisibility visibilityLevel = getVisibilityLevel(caller, key, subjects, profile);
            LOGGER.log(Level.FINE, "Visibility level set to " + visibilityLevel);

            List<ProfileData> visibleInfos = new ArrayList<ProfileData>();
            for (ProfileData info : profile.getInfos().values()) {
                LOGGER.log(Level.FINE, "Treating info " + info.getName());
                if (visibilityLevel.compareTo(info.getVisibility()) >= 0) {
                    LOGGER.log(Level.FINE, "info is visible");
                    if (filter != null && filter.length() > 0) {
                        if (info.getName().matches(filter + "(.*)")) {
                            LOGGER.log(Level.FINE, "info name matches filter");
                            visibleInfos.add(info);
                        }
                    } else {
                        LOGGER.log(Level.FINE, "filter is null or empty, adding info");
                        visibleInfos.add(info);
                    }
                }
            }

            return visibleInfos;
        } catch (RegistryServiceException | AuthorisationServiceException e) {
            throw new MembershipServiceException("unable to listy profile infos for profile with key [" + key + "]", e);
        }
    }

    private ProfileDataVisibility getVisibilityLevel(String caller, String key, List<String> subjects, Profile profile) throws AuthorisationServiceException, MembershipServiceException,
            KeyNotFoundException, RegistryServiceException {
        ProfileDataVisibility visibilityLevel = ProfileDataVisibility.EVERYBODY;
        try {
            authorisation.checkOwnership(key, subjects);
            visibilityLevel = ProfileDataVisibility.NOBODY;
        } catch (AccessDeniedException e1) {
            if (profile.getFriends() != null) {
                String friendsGroupKey = profile.getFriends();
                OrtolangObjectIdentifier friendsObject = registry.lookup(friendsGroupKey);
                checkObjectType(friendsObject, Group.OBJECT_TYPE);
                try {
                    if (isMember(friendsGroupKey, caller)) {
                        visibilityLevel = ProfileDataVisibility.FRIENDS;
                    }
                } catch (AccessDeniedException e2) {
                    LOGGER.log(Level.FINE, caller + " is not authorized to read friend list of profile with key [" + key + "]");
                }
            }
        }
        return visibilityLevel;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setProfileInfo(String key, String name, String value, ProfileDataVisibility visibility, ProfileDataType type, String source) throws MembershipServiceException, KeyNotFoundException,
            AccessDeniedException {
        LOGGER.log(Level.FINE, "set profile info [" + name + "] for profile with key [" + key + "]");
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
            if (value != null && value.length() > 0) {
                info = new ProfileData(name, value, visibility, type, source);
            }
            profile.setInfo(name, info);
            em.merge(profile);
            registry.update(key);
            indexing.index(key);

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("name", name);
            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "update-infos"), argumentsBuilder.build());
        } catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException | KeyLockedException | IndexingServiceException e) {
            throw new MembershipServiceException("unable to set profile info for profile with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Profile updateProfile(String key, String givenName, String familyName, String email, ProfileDataVisibility emailVisibility) throws MembershipServiceException, KeyNotFoundException,
            AccessDeniedException {
        LOGGER.log(Level.FINE, "updating profile for key [" + key + "]");
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
            profile.setEmailHash(hashEmail(email));
            if (emailVisibility != null) {
                profile.setEmailVisibility(emailVisibility);
            }
            em.merge(profile);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "update"));
            return profile;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | NoSuchAlgorithmException | UnsupportedEncodingException
                | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("error while trying to update the profile with key [" + key + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "deleting profile for key [" + key + "]");
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
            indexing.remove(key);

            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to delete object with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "adding public key to profile with key [" + key + "]");
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

            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "add-ssh-key"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("error while trying to add public key to profile with key [" + key + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "removing public key to profile with key [" + key + "]");
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

            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "remove-ssh-key"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("error while trying to remove public key to profile with key [" + key + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String generateConnectedIdentifierTOTP() throws MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "generating TOTP for connected identifier");
        try {
            String key = getProfileKeyForConnectedIdentifier();
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Profile.OBJECT_TYPE);
            Profile profile = em.find(Profile.class, identifier.getId());
            if (profile == null) {
                throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
            }
            if (profile.getSecret() == null || profile.getSecret().length() <= 0) {
                String secret = TOTPHelper.generateSecret();
                profile.setSecret(secret);
                em.merge(profile);
            }

            return Integer.toString(TOTPHelper.getCode(profile.getSecret()));
        } catch (RegistryServiceException e) {
            throw new MembershipServiceException("unable to generate TOTP for connected identifier", e);
        }
    }

    @Override
    @RolesAllowed("admin")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean systemValidateTOTP(String identifier, String totp) throws MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# validating TOTP for identifier");
        try {
            String key = getProfileKeyForIdentifier(identifier);
            OrtolangObjectIdentifier oid = registry.lookup(key);
            checkObjectType(oid, Profile.OBJECT_TYPE);
            Profile profile = em.find(Profile.class, oid.getId());
            if (profile == null) {
                throw new MembershipServiceException("unable to find a profile for id " + oid.getId());
            }
            return profile.getSecret() != null && profile.getSecret().length() > 0 && TOTPHelper.checkCode(profile.getSecret(), Long.parseLong(totp));
        } catch (RegistryServiceException e) {
            throw new MembershipServiceException("unable to validate TOTP for identifier: " + identifier, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Profile systemReadProfile(String identifier) throws MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# reading profile");
        try {
            String key = getProfileKeyForIdentifier(identifier);
            OrtolangObjectIdentifier oid = registry.lookup(key);
            checkObjectType(oid, Profile.OBJECT_TYPE);
            Profile profile = em.find(Profile.class, oid.getId());
            if (profile == null) {
                throw new MembershipServiceException("unable to find a profile for id " + oid.getId());
            }
            profile.setKey(key);
            return profile;
        } catch (RegistryServiceException e) {
            throw new MembershipServiceException("unable to read profile for identifier: " + identifier, e);
        }
    }

    @Override
    @RolesAllowed({"admin", "system"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void systemSetProfileReferentialId(String identifier, String referentialId) throws MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# set profile referential ID");
        try {
            Profile profile = systemReadProfile(identifier);
            profile.setReferentialId(referentialId);
            em.merge(profile);
            registry.update(profile.getKey());

            notification.throwEvent(profile.getKey(), "system", Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "update"));
        } catch (RegistryServiceException | NotificationServiceException | KeyLockedException e) {
            throw new MembershipServiceException("unable to set profile referential ID for identifier: " + identifier, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException, AccessDeniedException {
        LOGGER.log(Level.FINE, "creating group for key [" + key + "] and name [" + name + "]");
        try {
            String caller = getProfileKeyForConnectedIdentifier();
            List<String> subjects = getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            if (key.equals(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY)) {
                throw new MembershipServiceException("key [" + key + "] is reserved for all authentified users and cannot be used for a group");
            }

            Group group = new Group();
            group.setId(UUID.randomUUID().toString());
            group.setName(name);
            group.setDescription(description);
            em.persist(group);

            registry.register(key, group.getObjectIdentifier(), caller);

            authorisation.createPolicy(key, caller);
            Map<String, List<String>> rules = new HashMap<String, List<String>>();
            rules.put(key, Collections.singletonList("read"));
            authorisation.setPolicyRules(key, rules);

            indexing.index(key);

            notification.throwEvent(key, caller, Group.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "create"));
        } catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyNotFoundException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to create group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading group for key [" + key + "]");
        try {
            List<String> subjects = getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Group.OBJECT_TYPE);
            Group group = em.find(Group.class, identifier.getId());
            if (group == null) {
                throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
            }
            group.setKey(key);

            return group;
        } catch (RegistryServiceException | AuthorisationServiceException e) {
            throw new MembershipServiceException("unable to read the group with key [" + key + "]", e);
        }
    }

    @Override
    @RolesAllowed({"admin", "system"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Group systemReadGroup(String key) throws MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# reading group for key [" + key + "]");
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Group.OBJECT_TYPE);
            Group group = em.find(Group.class, identifier.getId());
            if (group == null) {
                throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
            }
            group.setKey(key);
            return group;
        } catch (RegistryServiceException e) {
            throw new MembershipServiceException("unable to read group for key: " + key, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateGroup(String key, String name, String description) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "updating group for key [" + key + "]");
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
            indexing.index(key);

            notification.throwEvent(key, caller, Group.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("error while trying to update the group with key [" + key + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "deleting group for key [" + key + "]");
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
            for (String pkey : group.getMembers()) {
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
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "unable to update profile key [" + pkey + "]", e);
                }
            }

            indexing.remove(key);
            registry.delete(key);

            notification.throwEvent(key, caller, Group.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to delete group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Group addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "adding member in group for key [" + key + "] and member [" + member + "]");
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
            indexing.index(member);
            indexing.index(key);

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("member", member);
            notification.throwEvent(key, caller, Group.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "add-member"), argumentsBuilder.build());
            return group;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to add member in group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "removing member [" + member + "] from group with key [" + key + "]");
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
            indexing.index(member);
            indexing.index(key);

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("member", member);
            notification.throwEvent(key, caller, Group.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, "remove-member"), argumentsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to remove member from group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "joining group with key [" + key + "]");
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
            indexing.index(caller);

            indexing.index(key);

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("member", caller);
            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "add-member"), argumentsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to join group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "leaving group with key [" + key + "]");
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
            indexing.index(caller);
            indexing.index(key);

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("member", caller);
            notification.throwEvent(key, caller, Profile.OBJECT_TYPE, buildEventType(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, "remove-member"), argumentsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MembershipServiceException("unable to leave group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "listing members of group with key [" + key + "]");
        try {
            List<String> subjects = getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Group.OBJECT_TYPE);

            Group group = em.find(Group.class, identifier.getId());
            if (group == null) {
                throw new MembershipServiceException("unable to find a group for id " + identifier.getId());
            }
            String[] members = group.getMembers();

            return Arrays.asList(members);
        } catch (RegistryServiceException | AuthorisationServiceException e) {
            throw new MembershipServiceException("unable to list members in group with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> getProfileGroups(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "listing groups of profile with key [" + key + "]");
        try {
            List<String> subjects = getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Profile.OBJECT_TYPE);

            Profile profile = em.find(Profile.class, identifier.getId());
            if (profile == null) {
                throw new MembershipServiceException("unable to find a profile for id " + identifier.getId());
            }
            String[] groups = profile.getGroups();

            return Arrays.asList(groups);
        } catch (RegistryServiceException | AuthorisationServiceException e) {
            throw new MembershipServiceException("unable to list groups of profile with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isMember(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "checking membership of member [" + member + "] in group with key [" + key + "]");
        try {
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

            return isMember;
        } catch (RegistryServiceException | AuthorisationServiceException e) {
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
        } catch (MembershipServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        LOGGER.log(Level.FINE, "calculating size for object with key [" + key + "]");
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
                ortolangObjectSize.addElements("friends", readGroup(profile.getFriends()).getMembers().length);
                break;
            }
            }
            return ortolangObjectSize;
        } catch (MembershipServiceException | RegistryServiceException | AuthorisationServiceException | AccessDeniedException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error while calculating object size", e);
            throw new OrtolangException("unable to calculate size for object with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexablePlainTextContent content = new IndexablePlainTextContent();

            if (identifier.getType().equals(Group.OBJECT_TYPE)) {
                Group group = em.find(Group.class, identifier.getId());
                if (group == null) {
                    throw new OrtolangException("unable to load group with id [" + identifier.getId() + "] from storage");
                }
                if (group.getName() != null) {
                    content.setName(group.getName());
                    content.addContentPart(group.getName());
                }
                if (group.getDescription() != null) {
                    content.addContentPart(group.getDescription());
                }
                if (group.getMembersList().length() > 0 ) {
                    content.addContentPart(group.getMembersList());
                }
            }

            if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
                Profile profile = em.find(Profile.class, identifier.getId());
                if (profile == null) {
                    throw new OrtolangException("unable to load profile with id [" + identifier.getId() + "] from storage");
                }
                if (profile.getFullName() != null) {
                    content.setName(profile.getFullName());
                    content.addContentPart("fullname", profile.getFullName(), TextField.TYPE_STORED);
                }
                if (profile.getEmail() != null && profile.getEmail().length() > 0) {
                    if (profile.getEmailVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                        content.addContentPart("email", profile.getEmail(), StringField.TYPE_STORED);
                    }
                }
                for (ProfileData info : profile.getInfos().values()) {
                    if (info.getVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                        content.addContentPart(info.getName(), info.getValue());
                    }
                }
            }

            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            IndexableJsonContent content = new IndexableJsonContent();

            if (identifier.getType().equals(Profile.OBJECT_TYPE)) {
                Profile profile = em.find(Profile.class, identifier.getId());
                if (profile == null) {
                    throw new OrtolangException("unable to load profile with id [" + identifier.getId() + "] from storage");
                }
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("key", key);
                builder.add("givenName", profile.getGivenName());
                builder.add("familyName", profile.getFamilyName());
                builder.add("fullname", profile.getFullName());
                builder.add("emailHash", profile.getEmailHash());
                if (profile.getEmail() != null && profile.getEmail().length() > 0) {
                    if (profile.getEmailVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                        builder.add("email", profile.getEmail());
                    }
                }
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String group : profile.getGroups()) {
                    arrayBuilder.add(group);
                }
                builder.add("groups", arrayBuilder);
                JsonObjectBuilder infoBuilder = Json.createObjectBuilder();
                Map<String, ProfileData> infos = profile.getInfos();
                for(Map.Entry<String, ProfileData> info : infos.entrySet()) {
                    if(info.getValue().getVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                        infoBuilder.add(info.getKey(), info.getValue().getValue());
                    }
                }
                builder.add("infos", infoBuilder);
                content.put(Profile.OBJECT_TYPE, builder.build().toString());
            }

            if (identifier.getType().equals(Group.OBJECT_TYPE)) {
                Group group = em.find(Group.class, identifier.getId());
                if (group == null) {
                    throw new OrtolangException("unable to load group with id [" + identifier.getId() + "] from storage");
                }
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("key", key);
                builder.add("name", group.getName());
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String member : group.getMembers()) {
                    arrayBuilder.add(member);
                }
                builder.add("members", arrayBuilder);
                content.put(Group.OBJECT_TYPE, builder.build().toString());
            }
            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public Map<String, Object> getElasticSearchContent(String key) throws KeyNotFoundException, RegistryServiceException, OrtolangException {
        OrtolangObjectIdentifier identifier = registry.lookup(key);

        if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        Map<String, Object> map = new HashMap<>();

        switch (identifier.getType()) {
        case Profile.OBJECT_TYPE:
            Profile profile = em.find(Profile.class, identifier.getId());
            map.put("key", key);
            map.put("givenName", profile.getGivenName());
            map.put("familyName", profile.getFamilyName());
            map.put("emailHash", profile.getEmailHash());
            if (profile.getEmailVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                map.put("email", profile.getEmail());
            }
            map.put("groups", profile.getGroups());
            Map<String, Object> infos = new HashMap<>();
            for (Map.Entry<String, ProfileData> info : profile.getInfos().entrySet()) {
                if (info.getValue().getVisibility().equals(ProfileDataVisibility.EVERYBODY)) {
                    infos.put(info.getKey(), info.getValue().getValue());
                }
            }
            map.put("infos", infos);
            break;
        case Group.OBJECT_TYPE:
            break;
        }
        return map;

    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            long nbprofiles = registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE), null);
            infos.put(MembershipService.INFO_PROFILES_ALL, Long.toString(nbprofiles));
        } catch (RegistryServiceException e) {
            //
        }
        try {
            long nbprofiles = registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE), null);
            infos.put(MembershipService.INFO_GROUPS_ALL, Long.toString(nbprofiles));
        } catch (RegistryServiceException e) {
            //
        }
        return infos;
    }

    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws MembershipServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new MembershipServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new MembershipServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }

    private String hashEmail(String email) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hashedBytes = digest.digest(email.getBytes("UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashedBytes) {
            stringBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuilder.toString();
    }

}
