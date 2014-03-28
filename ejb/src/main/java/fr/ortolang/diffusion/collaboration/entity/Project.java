package fr.ortolang.diffusion.collaboration.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@SuppressWarnings("serial")
public class Project extends OrtolangObject {

	public static final String OBJECT_TYPE = "project";

	@Id
	private String id;
	@Transient
	private String key;
	private String type;
	private String name;
	private String root;
	private String members;
	@ElementCollection(fetch=FetchType.EAGER)
	private List<String> versions;
	
	public Project() {
		versions = new ArrayList<String>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public List<String> getHistory() {
		return versions;
	}

	public void setHistory(List<String> versions) {
		this.versions = versions;
	}

	public void addVersion(String version) {
		this.versions.add(version);
	}

	public String getMembers() {
		return members;
	}

	public void setMembers(String members) {
		this.members = members;
	}
	
	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Project.OBJECT_TYPE, id);
	}

}
