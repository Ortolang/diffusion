package fr.ortolang.diffusion.api.profile;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "profile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileRepresentation {

	@XmlAttribute(name = "key")
	private String key;
	private String givenName;
	private String familyName;
	private String email;
	private String emailHash;
	private ProfileDataVisibility emailVisibility;
	private boolean emailVerified;
	private String status;
	private String[] groups;
	private boolean complete;
	private String friends;
	
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

	public String getEmailHash() {
		return emailHash;
	}

	public void setEmailHash(String emailHash) {
		this.emailHash = emailHash;
	}

	public ProfileDataVisibility getEmailVisibility() {
		return emailVisibility;
	}

	public void setEmailVisibility(ProfileDataVisibility emailVisibility) {
		this.emailVisibility = emailVisibility;
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

	public String getFriends() {
		return friends;
	}
	
	public void setFriends(String friends) {
		this.friends = friends;		
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
		representation.setEmailHash(profile.getEmailHash());
		representation.setEmailVisibility(profile.getEmailVisibility());
		representation.setEmailVerified(profile.isEmailVerified());
		representation.setGivenName(profile.getGivenName());
		representation.setFamilyName(profile.getFamilyName());
		representation.setStatus(profile.getStatus().name());
		representation.setGroups(profile.getGroups());
		representation.setComplete(profile.isComplete());
		representation.setFriends(profile.getFriends());
		return representation;
	}

}
