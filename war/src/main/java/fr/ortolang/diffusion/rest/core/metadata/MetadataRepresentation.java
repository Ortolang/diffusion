package fr.ortolang.diffusion.rest.core.metadata;

import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.rest.DiffusionRepresentation;

public class MetadataRepresentation extends DiffusionRepresentation { 

	private String key;
	private String name;
	private String size;
	private String contentType;
	private String target;
	private String format;
	
	public MetadataRepresentation() {
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
	
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public static MetadataRepresentation fromMetadataObject(MetadataObject meta) {
		MetadataRepresentation representation = new MetadataRepresentation();
		representation.setKey(meta.getKey());
		representation.setName(meta.getName());
		representation.setContentType(meta.getContentType());
		representation.setSize(meta.getSize()+"");
		representation.setTarget(meta.getTarget());
		representation.setFormat(meta.getFormat());
		return representation;
	}
}
