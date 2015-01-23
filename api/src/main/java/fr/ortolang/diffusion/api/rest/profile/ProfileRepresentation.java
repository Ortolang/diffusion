package fr.ortolang.diffusion.api.rest.profile;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.membership.entity.Profile;

@XmlRootElement(name = "profile")
public class ProfileRepresentation {

	@XmlAttribute(name = "key")
	private String key;
	private String givenName;
	private String familyName;
	private String email;
	private boolean emailVerified;
	private String status;
	private String[] groups;
	private boolean complete;

	public ProfileRepresentation() {
		groups = new String[0];
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String[] getGroups() {
		return groups;
	}

	public void setGroups(String[] groups) {
		this.groups = groups;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public static ProfileRepresentation fromProfile(Profile profile) {
		ProfileRepresentation representation = new ProfileRepresentation();
		representation.setKey(profile.getKey());
		representation.setEmail(profile.getEmail());
		representation.setEmailVerified(profile.isEmailVerified());
		representation.setGivenName(profile.getGivenName());
		representation.setFamilyName(profile.getFamilyName());
		representation.setStatus(profile.getStatus().name());
		representation.setGroups(profile.getGroups());
		representation.setComplete(profile.isComplete());
		return representation;
	}

}
