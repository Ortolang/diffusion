package fr.ortolang.diffusion.membership.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.Version;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.membership.MembershipService;

@Entity
@SuppressWarnings("serial")
public class Profile extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "profile";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String givenName;
	private String familyName;
	private String email;
	private boolean emailVerified;
	@Column(length=7000)
	private String groupsList;
	private ProfileStatus status;
	@ElementCollection
	private Set<ProfileKey> keys;
	
	public Profile() {
		groupsList = "";
		keys = new HashSet<ProfileKey> ();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Transient
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public String getFullName() {
		return givenName + " " + familyName;
	}

	public ProfileStatus getStatus() {
		return status;
	}

	public void setStatus(ProfileStatus status) {
		this.status = status;
	}

	public String getGroupsList() {
		return groupsList;
	}

	public void setGroupsList(String groupsList) {
		this.groupsList = groupsList;
	}

	public boolean isMemberOf(String group) {
		if (groupsList.indexOf(group) != -1) {
			return true;
		}

		return false;
	}

	public boolean isComplete() {
		return givenName != null && !givenName.isEmpty()
				&& familyName != null && !familyName.isEmpty()
				&& email != null && !email.isEmpty();
	}

	public void addPublicKey(String pubkey) {
		keys.add(new ProfileKey(pubkey, pubkey.split(" ")[1]));
	}

	public void removePublicKey(String pubkey) {
		keys.remove(new ProfileKey(pubkey, pubkey.split(" ")[1]));
	}

	public Set<String> getPublicKeys() {
		Set<String> pkeys = new HashSet<String> ();
		for ( ProfileKey pkey : keys ) {
			pkeys.add(pkey.getKey());
		}
		return pkeys;
	}
	
	public void setPublicKeys(Set<String> keys) {
		for ( String key : keys ) {
			addPublicKey(key);
		}
	}
	
	public Set<ProfileKey> getKeys() {
		return keys;
	}

	public void setKeys(Set<ProfileKey> keys) {
		this.keys = keys;
	}

	public void addGroup(String group) {
		if (!isMemberOf(group)) {
			if (groupsList.length() > 0) {
				groupsList += ("," + group);
			} else {
				groupsList += group;
			}
		}
	}

	public void removeGroup(String group) {
		if (isMemberOf(group)) {
			groupsList = groupsList.replaceAll("(" + group + "){1},?", "");
		}
	}

	public String[] getGroups() {
		if (groupsList.equals("")) {
			return new String[0];
		}

		return groupsList.split(",");
	}
	
	@Override
	public String getObjectKey() {
		return getKey();
	}
	
	@Override
	public String getObjectName() {
		return getFullName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, id);
	}
}