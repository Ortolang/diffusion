package fr.ortolang.diffusion.membership.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.membership.MembershipService;

@Entity
@Table(name = "`GROUP`")
@SuppressWarnings("serial")
public class Group extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "group";
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String description;
	private String membersList;

	public Group() {
		membersList = "";
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMembersList() {
		return membersList;
	}

	public void setMembersList(String membersList) {
		this.membersList = membersList;
	}

	public boolean isMember(String member) {
		if (membersList.indexOf(member) != -1) {
			return true;
		}

		return false;
	}

	public void addMember(String member) {
		if (!isMember(member)) {
			if (membersList.length() > 0) {
				membersList += ("," + member);
			} else {
				membersList += member;
			}
		}
	}

	public void removeMember(String member) {
		if (isMember(member)) {
			membersList = membersList.replaceAll("(" + member + "){1},?", "");
		}
	}

	public String[] getMembers() {
		if (membersList.equals("")) {
			return new String[0];
		}

		return membersList.split(",");
	}
	
	@Override
	public String getObjectKey() {
		return getKey();
	}
	
	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, id);
	}

}