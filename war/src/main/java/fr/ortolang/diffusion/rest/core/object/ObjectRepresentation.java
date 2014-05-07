package fr.ortolang.diffusion.rest.core.object;

import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.rest.DiffusionRepresentation;

public class ObjectRepresentation extends DiffusionRepresentation {

	private String key;
	private String name;
	private String description;
	private String size;
	private String contentType;
	private String preview;
	private String nbReads;
	
	public ObjectRepresentation() {
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
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public String getNbReads() {
		return nbReads;
	}

	public void setNbReads(String nbReads) {
		this.nbReads = nbReads;
	}

	public static ObjectRepresentation fromDataObject(DataObject object) {
		ObjectRepresentation representation = new ObjectRepresentation();
		representation.setKey(object.getKey());
		representation.setName(object.getName());
		representation.setDescription(object.getDescription());
		representation.setContentType(object.getContentType());
		representation.setSize(object.getSize()+"");
		representation.setNbReads(object.getNbReads()+"");
		representation.setPreview(object.getPreview());
		return representation;
	}
	
}
