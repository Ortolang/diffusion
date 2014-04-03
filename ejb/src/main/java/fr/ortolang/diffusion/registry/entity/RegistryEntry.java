package fr.ortolang.diffusion.registry.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import fr.ortolang.diffusion.OrtolangObjectState;

@Entity
@Table(indexes = {@Index(columnList="identifier")})
@NamedQueries({
	@NamedQuery(name="findEntryByIdentifier", query="select e from RegistryEntry e where e.identifier = :identifier"),
	@NamedQuery(name="listVisibleKeys", query="select e.key from RegistryEntry e where e.hidden = false and e.deleted = false and e.identifier like :filter"),
	@NamedQuery(name="countVisibleKeys", query="select count(e) from RegistryEntry e where e.hidden = false and e.deleted = false and e.identifier like :filter")
})
@SuppressWarnings("serial")
public class RegistryEntry implements Serializable {

	@Id
	private String key;
	private boolean hidden;
	private boolean deleted;
	private String lock;
	private String publicationStatus;
	private String identifier;
	private String parent;
	@ElementCollection(fetch=FetchType.EAGER)
	private List<String> children;
	@ElementCollection(fetch=FetchType.EAGER)
	private Map<String, String> properties;
	@ElementCollection(fetch=FetchType.EAGER)
	private List<String> tags;

	public RegistryEntry() {
		hidden = false;
		deleted = false;
		lock = "";
		publicationStatus = OrtolangObjectState.Status.DRAFT.value();
		parent = null;
		children = new ArrayList<String> ();
		properties = new HashMap<String, String> ();
		tags = new ArrayList<String> ();
	}

	public RegistryEntry(String key, String identifier) {
		this();
		this.key = key;
		this.identifier = identifier;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public boolean isLocked() {
		return lock.length() > 0;
	}

	public void setLock(String owner) {
		this.lock = owner;
	}
	
	public String getLock() {
		return this.lock;
	}
	
	public String getPublicationStatus() {
		return publicationStatus;
	}

	public void setPublicationStatus(String publicationStatus) {
		this.publicationStatus = publicationStatus;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}
	
	public void addChildren(String child) {
		this.children.add(child);
	}
	
	public boolean hasChildren() {
		if (children.size() > 0) {
			return true;
		}
		return false;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public void setProperty(String name, String value) {
		if ( value == null || value.equals("") ) {
			this.properties.remove(name);
		}
		this.properties.put(name, value);
	}
	
	public void removeProperty(String name) {
		this.properties.remove(name);
	}
	
	public List<String> getTags() {
		return tags;
	}
	
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	public void removeTag(String tag) {
		this.tags.remove(tag);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{key:").append(getKey());
		buffer.append(", locked:").append(isLocked());
		buffer.append(", hidden:").append(isHidden());
		buffer.append(", deleted:").append(isDeleted());
		buffer.append(", parent:").append(getParent());
		buffer.append(", identifier:").append(getIdentifier());
		buffer.append("}");
		return buffer.toString();
	}

}
