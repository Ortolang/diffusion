package fr.ortolang.diffusion.rest.api;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.rest.DiffusionRepresentation;

public class OrtolangObjectRepresentation extends DiffusionRepresentation {
	
	private String key;
	private String service;
	private String type;
	private String id;
	
	public OrtolangObjectRepresentation() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public static OrtolangObjectRepresentation fromOrtolangObjectIdentifier(OrtolangObjectIdentifier identifier) {
		OrtolangObjectRepresentation representation = new OrtolangObjectRepresentation();
		representation.setService(identifier.getService());
		representation.setType(identifier.getType());
		representation.setId(identifier.getId());
		return representation;
	}
	
}
