package fr.ortolang.diffusion.core.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({ @NamedQuery(name = "listMetadataFormat", query = "select f from MetadataFormat f"),
		@NamedQuery(name = "findMetadataFormatForName", query = "select f from MetadataFormat f where f.name = :name order by f.serial desc") })
public class MetadataFormat {
	
	public static final String ACL = "ortolang-acl-json";
	public static final String ITEM = "ortolang-item-json";
	public static final String ORGANIZATION = "ortolang-organization-json";
	public static final String WORKSPACE = "ortolang-workspace-json";
	

	@Id
	private String id;
	@Version
	private long version;
	private int serial;
	private String name;
	@Column(length = 2500)
	private String description;
	private long size;
	private String mimeType;
	private String schema;
	private String form;

	public MetadataFormat() {
		serial = 1;
	}

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String form) {
		this.form = form;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public int getSerial() {
		return serial;
	}

	public void setSerial(int serial) {
		this.serial = serial;
	}

}
