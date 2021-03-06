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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.membership.MembershipService;

@Entity
@NamedQueries({
        @NamedQuery(name = "listAllProfiles", query = "SELECT p FROM Profile p"),
})
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
    private String emailHash;
    private ProfileDataVisibility emailVisibility;
    private boolean emailVerified;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String groupsList;
    private String friends;
    private ProfileStatus status;
    private String secret;
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<ProfileKey> keys;
    @ElementCollection(fetch = FetchType.LAZY)
    private Map<String, ProfileData> infos;
    private String referentialId;

    public Profile() {
        groupsList = "";
        keys = new HashSet<>();
        infos = new HashMap<>();
        emailVisibility = ProfileDataVisibility.FRIENDS;
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

    public String getFullName() {
        return givenName + " " + familyName;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileStatus status) {
        this.status = status;
    }

    @JsonIgnore
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
        return Arrays.asList(groupsList.split(",")).contains(group);
    }

    public boolean isComplete() {
        return givenName != null && !givenName.isEmpty() && familyName != null && !familyName.isEmpty() && email != null && !email.isEmpty();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void addPublicKey(String pubkey) {
        keys.add(new ProfileKey(pubkey, pubkey.split(" ")[1]));
    }

    public void removePublicKey(String pubkey) {
        keys.remove(new ProfileKey(pubkey, pubkey.split(" ")[1]));
    }

    @JsonIgnore
    public Set<String> getPublicKeys() {
        Set<String> pkeys = new HashSet<String>();
        for (ProfileKey pkey : keys) {
            pkeys.add(pkey.getKey());
        }
        return pkeys;
    }

    public void setPublicKeys(Set<String> keys) {
        keys.forEach(this::addPublicKey);
    }

    @JsonIgnore
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
            groupsList = groupsList.replaceAll("(" + group + "),?", "");
        }
        if ( groupsList.endsWith(",") ) {
            groupsList = groupsList.substring(0, groupsList.length()-1);
        }
    }

    @JsonIgnore
    public String[] getGroups() {
        if (groupsList.isEmpty()) {
            return new String[0];
        }
        return groupsList.split(",");
    }

    @JsonIgnore
    public Map<String, ProfileData> getInfos() {
        return infos;
    }

    public void setInfos(Map<String, ProfileData> infos) {
        this.infos = infos;
    }

    public ProfileData getInfo(String name) {
        return infos.get(name);
    }

    public void setInfo(String name, ProfileData info) {
        this.infos.put(name, info);
    }

    public String getReferentialId() {
        return referentialId;
    }

    public void setReferentialId(String referentialId) {
        this.referentialId = referentialId;
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
