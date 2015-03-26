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
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface MembershipService extends OrtolangService, OrtolangIndexableService {
	
	public static final String SERVICE_NAME = "membership";
	
	public static final String UNAUTHENTIFIED_IDENTIFIER = "anonymous";
	public static final String SUPERUSER_IDENTIFIER = "root";
	
	public static final String ALL_AUTHENTIFIED_GROUP_KEY = "authentified";
	public static final String ADMIN_GROUP_KEY = "admins";
	public static final String MODERATOR_GROUP_KEY = "moderators";
	public static final String ESR_GROUP_KEY = "esr";

	public String getProfileKeyForConnectedIdentifier();

	public String getProfileKeyForIdentifier(String identifier);

	public List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException;

	public void createProfile(String identifier, String givenName, String familyName, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException;
	
	public Profile createProfile(String givenName, String familyName, String email) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException;

	public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void updateProfile(String key, String givenName, String familyName, String email) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void addProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void removeProfilePublicKey(String key, String pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public List<ProfileData> listProfileInfos(String key, String filter) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void setProfileInfo(String key, String name, String value, int visibility, ProfileDataType type, String source) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public List<Profile> listProfiles() throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public List<Profile> searchProfile(String data) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	
	public void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void updateGroup(String key, String name, String description) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public List<String> getProfileGroups(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public boolean isMember(String key, String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

}
