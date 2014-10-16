package fr.ortolang.diffusion.core.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

/**
 * A DigitalMetadata object contains a metadata file which is attached to a DigitalReference.
 * 
 * @author cyril pestel <cyril.pestel@atilf.fr>
 * 
 */
@Entity
@Table(indexes = {@Index(columnList="target")})
@NamedQueries({
	@NamedQuery(name="findMetadataObjectsForTarget", query="select r from MetadataObject r where r.target = :target")
})
@SuppressWarnings("serial")
public class MetadataObject extends OrtolangObject {

	public static final String OBJECT_TYPE = "metadata";

	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	private long size;
	private String contentType;
	private String stream;
	private String target;
	private String format; 
	
	public MetadataObject() {
		stream = "";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, id);
	}

}
