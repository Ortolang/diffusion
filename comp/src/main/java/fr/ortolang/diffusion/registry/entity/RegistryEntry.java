package fr.ortolang.diffusion.registry.entity;

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
@NamedQueries({
		@NamedQuery(name = "findEntryByIdentifier", query = "SELECT e FROM RegistryEntry e WHERE e.identifier = :identifier AND e.deleted = false"),
		@NamedQuery(name = "listVisibleKeys", query = "SELECT e.key FROM RegistryEntry e WHERE e.hidden = false AND e.deleted = false AND e.publicationStatus LIKE :statusFilter AND e.identifier LIKE :identifierFilter ORDER BY e.lastModificationDate DESC"),
		@NamedQuery(name = "countVisibleEntries", query = "SELECT count(e) FROM RegistryEntry e WHERE e.hidden = false AND e.deleted = false AND e.publicationStatus LIKE :statusFilter AND e.identifier LIKE :identifierFilter"),
		@NamedQuery(name = "listAllKeys", query = "SELECT e.key FROM RegistryEntry e WHERE e.identifier LIKE :identifierFilter"),
        @NamedQuery(name = "countAllEntries", query = "SELECT count(e) FROM RegistryEntry e WHERE e.identifier LIKE :identifierFilter"),
		@NamedQuery(name = "countDeletedEntries", query = "SELECT count(e) FROM RegistryEntry e WHERE e.deleted = true AND e.identifier LIKE :identifierFilter"),
		@NamedQuery(name = "countHiddenEntries", query = "SELECT count(e) FROM RegistryEntry e WHERE e.hidden = true AND e.identifier LIKE :identifierFilter"),
		@NamedQuery(name = "countPublishedEntries", query = "SELECT count(e) FROM RegistryEntry e WHERE e.hidden = false AND e.deleted = false AND e.publicationStatus = 'published' AND e.identifier LIKE :identifierFilter"),
		@NamedQuery(name = "findEntryByKey", query = "SELECT e FROM RegistryEntry e WHERE e.key LIKE :keyFilter"),
		@NamedQuery(name = "findEntryByKeyAndIdentifier", query = "SELECT e FROM RegistryEntry e WHERE e.key LIKE :keyFilter AND e.identifier LIKE :identifierFilter ORDER BY e.lastModificationDate DESC")})        
@SuppressWarnings("serial")
public class RegistryEntry implements Serializable, Comparable<RegistryEntry> {

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
		if (properties == null) {
			properties = new Properties();
			if (propertiesContent.length() > 0) {
				properties.load(new StringReader(propertiesContent));
			}
		}
		return properties;
	}

	public void setProperties(Properties properties) throws IOException {
		this.properties = properties;
		saveProperties();
	}

	private void saveProperties() throws IOException {
		if (properties != null && !properties.isEmpty()) {
			StringWriter output = new StringWriter();
			properties.store(output, null);
			propertiesContent = output.toString();
		}
	}
	
	
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((author == null) ? 0 : author.hashCode());
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + (int) (creationDate ^ (creationDate >>> 32));
        result = prime * result + (deleted ? 1231 : 1237);
        result = prime * result + (hidden ? 1231 : 1237);
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + (int) (lastModificationDate ^ (lastModificationDate >>> 32));
        result = prime * result + ((lock == null) ? 0 : lock.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((publicationStatus == null) ? 0 : publicationStatus.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RegistryEntry other = (RegistryEntry) obj;
        if (author == null) {
            if (other.author != null)
                return false;
        } else if (!author.equals(other.author))
            return false;
        if (children == null) {
            if (other.children != null)
                return false;
        } else if (!children.equals(other.children))
            return false;
        if (creationDate != other.creationDate)
            return false;
        if (deleted != other.deleted)
            return false;
        if (hidden != other.hidden)
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (lastModificationDate != other.lastModificationDate)
            return false;
        if (lock == null) {
            if (other.lock != null)
                return false;
        } else if (!lock.equals(other.lock))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (publicationStatus == null) {
            if (other.publicationStatus != null)
                return false;
        } else if (!publicationStatus.equals(other.publicationStatus))
            return false;
        return true;
    }

    @Override
    public int compareTo(RegistryEntry o) {
        return (int) (lastModificationDate - o.getLastModificationDate());
    }

    @Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{key:").append(getKey());
		builder.append(", locked:").append(isLocked());
		builder.append(", hidden:").append(isHidden());
		builder.append(", deleted:").append(isDeleted());
		builder.append(", parent:").append(getParent());
		builder.append(", identifier:").append(getIdentifier());
		builder.append(", author:").append(getAuthor());
		builder.append("}");
		return builder.toString();
	}

}