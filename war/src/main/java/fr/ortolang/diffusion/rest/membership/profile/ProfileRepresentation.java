package fr.ortolang.diffusion.rest.membership.profile;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.rest.api.OrtolangLinkableRepresentation;

@XmlRootElement(name="profile")
public class ProfileRepresentation extends OrtolangLinkableRepresentation {

	@XmlAttribute(name="key")
	private String key;
	private String email;
	private String fullname;
	private String status;

	public ProfileRepresentation() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public static ProfileRepresentation fromProfile(Profile profile) {
		ProfileRepresentation representation = new ProfileRepresentation();
		representation.setKey(profile.getKey());
		representation.setEmail(profile.getEmail());
		representation.setFullname(profile.getFullname());
		representation.setStatus(profile.getStatus().name());		
		return representation;
	}

}
