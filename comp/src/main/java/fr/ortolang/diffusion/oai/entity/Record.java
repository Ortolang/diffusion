package fr.ortolang.diffusion.oai.entity;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Type;

@Entity
@NamedQueries({
    @NamedQuery(name = "findRecordsByIdentifier", query = "SELECT r FROM Record r WHERE r.identifier = :identifier"),
	@NamedQuery(name = "findRecordsByMetadataPrefix", query = "SELECT r FROM Record r WHERE r.metadataPrefix = :metadataPrefix"),
	@NamedQuery(name = "listRecordsBySet", query = "SELECT r FROM Record r WHERE :set MEMBER OF r.sets"),
	@NamedQuery(name = "findRecordsByIdentifierAndMetadataPrefix", query = "SELECT r FROM Record r WHERE r.identifier = :identifier AND r.metadataPrefix = :metadataPrefix"),
	@NamedQuery(name = "countRecordsGroupByIdentifier", query = "SELECT COUNT(DISTINCT r.identifier) FROM Record r")
}
)
public class Record {

	@Id
	private String id;
	@Version
	private long version;
	
	private String identifier;
	private String metadataPrefix;
	private long lastModificationDate;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String xml;
    @Column(length=8000)
	@ElementCollection(fetch=FetchType.EAGER)
	private Set<String> sets;
	
	public Record() {
	}
	
	public Record(String id, String identifier, String metadataPrefix, long lastModificationDate, String xml) {
		this(id, identifier, metadataPrefix, lastModificationDate, xml, new HashSet<String>());
	}
	
	public Record(String id, String identifier, String metadataPrefix, long lastModificationDate, String xml, Set<String> sets) {
		this.id = id;
		this.identifier = identifier;
		this.metadataPrefix = metadataPrefix;
		this.lastModificationDate = lastModificationDate;
		this.xml = xml;
		this.sets = sets;
	}
	
	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getMetadataPrefix() {
		return metadataPrefix;
	}
	public void setMetadataPrefix(String metadataPrefix) {
		this.metadataPrefix = metadataPrefix;
	}
	public long getLastModificationDate() {
		return lastModificationDate;
	}
	public void setLastModificationDate(long lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}
	public String getXml() {
		return xml;
	}
	public void setXml(String xml) {
		this.xml = xml;
	}

    public Set<String> getSets() {
		return sets;
	}

	public void setSets(Set<String> sets) {
		this.sets = sets;
	}

	public long getVersion() {
        return version;
    }
    public void setVersion(long version) {
        this.version = version;
    }
}
