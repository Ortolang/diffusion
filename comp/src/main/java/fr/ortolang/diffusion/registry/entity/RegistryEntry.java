package fr.ortolang.diffusion.registry.entity;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObjectState;

@Entity
@Table(indexes = { @Index(columnList = "identifier") })
@NamedQueries({ @NamedQuery(name = "findEntryByIdentifier", query = "select e from RegistryEntry e where e.identifier = :identifier"),
		@NamedQuery(name = "listVisibleKeys", query = "select e.key from RegistryEntry e where e.hidden = false and e.deleted = false and e.identifier like :filter"),
		@NamedQuery(name = "countVisibleKeys", query = "select count(e) from RegistryEntry e where e.hidden = false and e.deleted = false and e.identifier like :filter") })
@SuppressWarnings("serial")
public class RegistryEntry implements Serializable {

	@Id
	private String key;
	@Version
	private long version;
	private boolean hidden;
	private boolean deleted;
	private String lock;
	private String publicationStatus;
	private String identifier;
	private String parent;
	private String children;
	private String author;
	private long creationDate;
	private long lastModificationDate;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String propertiesContent = "";
	@Transient
	private Properties properties;
	
	public RegistryEntry() {
		hidden = false;
		deleted = false;
		lock = "";
		publicationStatus = OrtolangObjectState.Status.DRAFT.value();
		author = null;
		parent = null;
		children = null;
		creationDate = -1;
		lastModificationDate = -1;
		properties = null;
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

	public String getChildren() {
		return children;
	}

	public void setChildren(String children) {
		this.children = children;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(long lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public void setPropertiesContent(String propertiesContent) {
		this.propertiesContent = propertiesContent;
	}
	
	public String getPropertiesContent() {
		return propertiesContent;
	}
	
	public void setProperty(String name, String value) throws IOException {
		getProperties().setProperty(name, value);
		saveProperties();
	}
	
	public Properties getProperties() throws IOException {
		if ( properties == null ) {
			properties = new Properties();
			if ( propertiesContent.length() > 0 ) {
				properties.load(new StringReader(propertiesContent));
			}
		}
		return properties;
	}
	
	private void saveProperties() throws IOException {
		if ( properties != null && !properties.isEmpty() ) {
			StringWriter output = new StringWriter();
			properties.store(output, null);
			propertiesContent = output.toString();
		}
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
		buffer.append(", author:").append(getAuthor());
		buffer.append("}");
		return buffer.toString();
	}

}