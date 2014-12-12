package fr.ortolang.diffusion.membership;

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface MembershipService extends OrtolangService, OrtolangIndexableService {
	
	public static final String SERVICE_NAME = "membership";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Group.OBJECT_TYPE, Profile.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Profile.OBJECT_TYPE, "read,update,delete" },
		{ Group.OBJECT_TYPE, "read,update,delete" }};
	
	public static final String UNAUTHENTIFIED_IDENTIFIER = "anonymous";
	public static final String SUPERUSER_IDENTIFIER = "root";
	
	public static final String ALL_AUTHENTIFIED_GROUP_KEY = "authentified";
	public static final String ADMIN_GROUP_KEY = "admins";
	public static final String MODERATOR_GROUP_KEY = "moderators";

	public String getProfileKeyForConnectedIdentifier();

	public String getProfileKeyForIdentifier(String identifier);

	public List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException;

	public void createProfile(String identifier, String fullname, String email, ProfileStatus status) throws MembershipServiceException, ProfileAlreadyExistsException, AccessDeniedException;
	
	public Profile createProfile(String fullname, String email) throws MembershipServiceException, ProfileAlreadyExistsException;

	public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateProfile(String key, String fullname, String email) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

	public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException;

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
