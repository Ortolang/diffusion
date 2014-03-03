package fr.ortolang.diffusion.core.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

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
@SuppressWarnings("serial")
public class DigitalMetadata extends OrtolangObject {

	public static final String OBJECT_TYPE = "metadata";

	@Id
	private String id;
	private String key;
	private String name;
	private long size;
	private String contentType;
	//private String preview; // ??
	// nbReads, description ??
	/**
	 * Hash from binary store (SHA-1).
	 */
	private String stream;
	/**
	 * Target must be a reference.
	 */
	private String target;
	/**
	 * It tells the metadata format. e.g : Dublin Core
	 */
	private String format; // ??

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
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, id);
	}

}
