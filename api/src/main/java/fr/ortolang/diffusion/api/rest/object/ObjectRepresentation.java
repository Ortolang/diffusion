package fr.ortolang.diffusion.api.rest.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.OrtolangObject;

@XmlRootElement(name = "object")
public class ObjectRepresentation {

	@XmlAttribute
	private String key;
	private String service;
	private String type;
	private String id;
	private String status;
	private String visibility;
	private String lock;
	private String author;
	private String creationDate;
	private String lastModificationDate;
	private Map<String, String> properties;
	private String owner;
	private Map<String, List<String>> permissions;
	private OrtolangObject object;

	public ObjectRepresentation() {
		properties = new HashMap<String, String>();
		permissions = new HashMap<String, List<String>>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLock() {
		return lock;
	}

	public void setLock(String lock) {
		this.lock = lock;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public Map<String, List<String>> getPermissions() {
		return permissions;
	}

	public void setPermissions(Map<String, List<String>> permissions) {
		this.permissions = permissions;
	}

	public OrtolangObject getObject() {
		return object;
	}

	public void setObject(OrtolangObject object) {
		this.object = object;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(String lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

}
