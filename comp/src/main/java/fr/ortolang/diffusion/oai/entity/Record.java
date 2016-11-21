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
	@NamedQuery(name = "findAllRecordsByMetadataPrefix", query = "SELECT r FROM Record r WHERE r.metadataPrefix = :metadataPrefix"),
	@NamedQuery(name = "findRecordBykey", query = "SELECT r FROM Record r WHERE r.key = :key")
}
)
public class Record {

	@Id
	private String key;
	@Version
	private long version;
	
	private String metadataPrefix;
	private long lastModificationDate;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String xml;
	
	public Record() {
	}
	
	public Record(String key, String metadataPrefix, long lastModificationDate, String xml) {
		this.key = key;
		this.metadataPrefix = metadataPrefix;
		this.lastModificationDate = lastModificationDate;
		this.xml = xml;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
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
