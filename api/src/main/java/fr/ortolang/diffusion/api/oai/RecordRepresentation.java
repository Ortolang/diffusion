package fr.ortolang.diffusion.api.oai;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.oai.entity.Record;

@XmlRootElement(name = "record")
public class RecordRepresentation {
	private String identifier;
    private String metadataPrefix;
    private long lastModificationDate;
    private String xml;
    private Set<String> sets;

	public RecordRepresentation() {}
    
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

	public static RecordRepresentation fromRecord(Record rec) {
		RecordRepresentation representation = new RecordRepresentation();
		representation.setIdentifier(rec.getIdentifier());
		representation.setLastModificationDate(rec.getLastModificationDate());
		representation.setMetadataPrefix(rec.getMetadataPrefix());
		representation.setSets(rec.getSets());
		representation.setXml(rec.getXml());
        return representation;
    }
    
}
