package fr.ortolang.diffusion.membership;

import java.util.List;

import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

public interface MembershipService {
	
	public static final String SERVICE_NAME = "membership";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Group.OBJECT_TYPE, Profile.OBJECT_TYPE };
	
	public static final String PROFILES_KEY_SUFFIX = "profiles:";
	public static final String GROUPS_KEY_SUFFIX = "groups:";
	public static final String UNAUTHENTIFIED_IDENTIFIER = "guest";
	public static final String SUPERUSER_IDENTIFIER = "root";

	public String getProfileKeyForConnectedIdentifier();

	public String getProfileKeyForIdentifier(String identifier);

	public List<String> getConnectedIdentifierSubjects() throws MembershipServiceException, KeyNotFoundException;

	public void createProfile(String identifier, String fullname, String email, String jabberid, int accountStatus) throws MembershipServiceException, ProfileAlreadyExistsException;

	public Profile findProfileByEmail(String email) throws MembershipServiceException, ProfileNotFoundException;

	public Profile readProfile(String key) throws MembershipServiceException, KeyNotFoundException;

	public void deleteProfile(String key) throws MembershipServiceException, KeyNotFoundException;

	public void updateProfile(String key, String fullname, String email) throws MembershipServiceException, KeyNotFoundException;

	public void createGroup(String key, String name, String description) throws MembershipServiceException, KeyAlreadyExistsException;

	public Group readGroup(String key) throws MembershipServiceException, KeyNotFoundException;

	public void updateGroup(String key, String name, String description) throws MembershipServiceException, KeyNotFoundException;

	public void deleteGroup(String key) throws MembershipServiceException, KeyNotFoundException;

	public void addMemberInGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException;

	public void removeMemberFromGroup(String key, String member) throws MembershipServiceException, KeyNotFoundException;

	public void joinGroup(String key) throws MembershipServiceException, KeyNotFoundException;

	public void leaveGroup(String key) throws MembershipServiceException, KeyNotFoundException;

	public List<String> listMembers(String key) throws MembershipServiceException, KeyNotFoundException;

	public List<String> getProfileGroups(String key) throws MembershipServiceException, KeyNotFoundException;

	public boolean isMember(String key, String member) throws MembershipServiceException, KeyNotFoundException;

}
