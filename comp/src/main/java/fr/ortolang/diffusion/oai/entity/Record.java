package fr.ortolang.diffusion.oai.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

@Entity
@NamedQueries({
    @NamedQuery(name = "findRecordsByIdentifier", query = "SELECT r FROM Record r WHERE r.identifier = :identifier"),
	@NamedQuery(name = "findRecordsByMetadataPrefix", query = "SELECT r FROM Record r WHERE r.metadataPrefix = :metadataPrefix"),
	@NamedQuery(name = "findRecordsByIdentifierAndMetadataPrefix", query = "SELECT r FROM Record r WHERE r.identifier = :identifier AND r.metadataPrefix = :metadataPrefix")
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
	
	public Record() {
	}
	
	public Record(String id, String identifier, String metadataPrefix, long lastModificationDate, String xml) {
		this.id = id;
		this.identifier = identifier;
		this.metadataPrefix = metadataPrefix;
		this.lastModificationDate = lastModificationDate;
		this.xml = xml;
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

    public long getVersion() {
        return version;
    }
    public void setVersion(long version) {
        this.version = version;
    }
}
