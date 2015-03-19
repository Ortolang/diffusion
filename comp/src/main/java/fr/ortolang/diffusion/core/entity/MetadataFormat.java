package fr.ortolang.diffusion.core.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.persistence.Version;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@NamedQueries({
	@NamedQuery(name="listMetadataFormat", query="select f from MetadataFormat f"),
	@NamedQuery(name="findMetadataFormatByName", query="select f from MetadataFormat f where f.name = :name")
})
@SuppressWarnings("serial")
public class MetadataFormat extends OrtolangObject {

	public static final String OBJECT_TYPE = "metadataFormat";
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	private String schema;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
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

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
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
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, MetadataFormat.OBJECT_TYPE, id);
	}

	
}
