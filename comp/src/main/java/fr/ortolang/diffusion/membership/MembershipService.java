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

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface MembershipService extends OrtolangService, OrtolangIndexableService {

    String SERVICE_NAME = "membership";

    String UNAUTHENTIFIED_IDENTIFIER = "anonymous";
    String SUPERUSER_IDENTIFIER = "root";

    String ALL_AUTHENTIFIED_GROUP_KEY = "authentified";
    String ADMINS_GROUP_KEY = "admins";
    String MODERATORS_GROUP_KEY = "moderators";
    String REVIEWERS_GROUP_KEY = "reviewers";
    String PUBLISHERS_GROUP_KEY = "publishers";
    String ESR_GROUP_KEY = "esr";

    String INFO_PROFILES_ALL = "profiles.all";
    String INFO_GROUPS_ALL = "groups.all";

    String getProfileKeyForConnectedIdentifier();

    String getProfileKeyForIdentifier(String identifier);

    String generateConnectedIdentifierTOTP() throws MembershipServiceException, KeyNotFoundException;

    List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException;

    Profile createProfile(String identifier, String givenName, String familyName, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException;

    Profile createProfile(String givenName, String familyName, String email) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException;

    Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    Profile updateProfile(String key, String givenName, String familyName, String email, ProfileDataVisibility emailVisibility) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void addProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void removeProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    List<ProfileData> listProfileInfos(String key, String filter) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void setProfileInfo(String key, String name, String value, ProfileDataVisibility visibility, ProfileDataType type, String source) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException, AccessDeniedException;

    Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void updateGroup(String key, String name, String description) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    Group addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    List<String> getProfileGroups(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    boolean isMember(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

    /* System */

    Group systemReadGroup(String key) throws MembershipServiceException, KeyNotFoundException;

    boolean systemValidateTOTP(String identifier, String totp) throws MembershipServiceException, KeyNotFoundException;

    Profile systemReadProfile(String identifier) throws MembershipServiceException, KeyNotFoundException;

    List<Profile> systemListProfiles() throws MembershipServiceException;

}
