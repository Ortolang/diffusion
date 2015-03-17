package fr.ortolang.diffusion.membership.entity;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

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
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String groupsList;
	private String friends;
	private ProfileStatus status;
	@ElementCollection
	private Set<ProfileKey> keys;
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, ProfileData> infos = new HashMap<String, ProfileData> ();
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, ProfileData> settings = new HashMap<String, ProfileData> ();
	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, ProfileData> aboutMe = new HashMap<String, ProfileData> ();
//	private ProfileData contributions;
	
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
	
	public String getFriends() {
		return friends;
	}

	public void setFriends(String friendsGroupKey) {
		this.friends = friendsGroupKey;
	}

	public boolean isMemberOf(String group) {
		if (groupsList.indexOf(group) != -1) {
			return true;
		}

		return false;
	}
	
	public boolean isFriendOf() {
		if (isMemberOf(friends)) {
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
		
	public Map<String,ProfileData> getInfos() {
		Map<String, ProfileData> infos = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.infos.entrySet()) {
			infos.put(entry.getValue().getName(), entry.getValue());
		}
		return infos;
	}
	
	public ProfileData getInfos(String name) {
		return this.infos.get(name);
	}
	
	public void updateInfo(String name, ProfileData data) {
		this.infos.put(name, data);
	}
	
	public void setInfos(Map<String, ProfileData> infos) {
		this.infos = infos;
	}
	
	public Map<String,ProfileData> getInfos(ProfileDataVisibility visibility) {
		Map<String, ProfileData> infosVisibles = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.infos.entrySet()) {
	    	ProfileDataVisibility dataVisibility = entry.getValue().getVisibility();
		    if(visibility == ProfileDataVisibility.NOBODY){
		    	infosVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.FRIENDS && 
		    		(dataVisibility == ProfileDataVisibility.FRIENDS || dataVisibility == ProfileDataVisibility.EVERYBODY)){
		    	infosVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.EVERYBODY && dataVisibility == ProfileDataVisibility.EVERYBODY){
		    	infosVisibles.put(entry.getValue().getName(), entry.getValue());
		    }
		}
		return infosVisibles;
	}
	
	public Map<String,ProfileData> getSettings() {
		Map<String, ProfileData> settings = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.settings.entrySet()) {
			settings.put(entry.getValue().getName(), entry.getValue());
		}
		return settings;
	}
	
	public ProfileData getSettings(String name) {
		return this.settings.get(name);
	}
	
	public void updateSettings(String name, ProfileData data) {
		this.settings.put(name, data);
	}
	
	public void setSettings(Map<String, ProfileData> settings) {
		this.settings = settings;
	}
	
	public Map<String,ProfileData> getSettings(ProfileDataVisibility visibility) {
		Map<String, ProfileData> settingsVisibles = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.settings.entrySet()) {
	    	ProfileDataVisibility dataVisibility = entry.getValue().getVisibility();
		    if(visibility == ProfileDataVisibility.NOBODY){
		    	settingsVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.FRIENDS && 
		    		(dataVisibility == ProfileDataVisibility.FRIENDS || dataVisibility == ProfileDataVisibility.EVERYBODY)){
		    	settingsVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.EVERYBODY && dataVisibility == ProfileDataVisibility.EVERYBODY){
		    	settingsVisibles.put(entry.getValue().getName(), entry.getValue());
		    }
		}
		return settingsVisibles;
	}
	
	public Map<String,ProfileData> getAboutMe() {
		Map<String, ProfileData> aboutMe = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.aboutMe.entrySet()) {
			aboutMe.put(entry.getValue().getName(), entry.getValue());
		}
		return aboutMe;
	}
	
	public ProfileData getAboutMe(String name) {
		return this.aboutMe.get(name);
	}
	
	public void updateAboutMe(String name, ProfileData data) {
		this.aboutMe.put(name, data);
	}
	
	public void setAboutMe(Map<String, ProfileData> aboutMe) {
		this.aboutMe = aboutMe;
	}
	
	public Map<String,ProfileData> getAboutMe(ProfileDataVisibility visibility) {
		Map<String, ProfileData> aboutMeVisibles = new HashMap<String, ProfileData>();
		for(Entry<String, ProfileData> entry : this.aboutMe.entrySet()) {
	    	ProfileDataVisibility dataVisibility = entry.getValue().getVisibility();
		    if(visibility == ProfileDataVisibility.NOBODY){
		    	aboutMeVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.FRIENDS && 
		    		(dataVisibility == ProfileDataVisibility.FRIENDS || dataVisibility == ProfileDataVisibility.EVERYBODY)){
		    	aboutMeVisibles.put(entry.getValue().getName(), entry.getValue());
		    } else if(visibility == ProfileDataVisibility.EVERYBODY && dataVisibility == ProfileDataVisibility.EVERYBODY){
		    	aboutMeVisibles.put(entry.getValue().getName(), entry.getValue());
		    }
		}
		return aboutMeVisibles;
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
