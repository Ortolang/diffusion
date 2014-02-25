package fr.ortolang.diffusion;

public class OrtolangIndexableObject {

	private OrtolangObjectIdentifier identifier;
	private String service;
	private String type;
	private String key;
	private String name;
	private OrtolangIndexableContent content;
	
	public OrtolangIndexableObject() {
	}

	public void setIdentifier(OrtolangObjectIdentifier identifier) {
		this.identifier = identifier;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setContent(OrtolangIndexableContent content) {
		this.content = content;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public OrtolangObjectIdentifier getIdentifier() {
		return identifier;
	}

	public String getService() {
		return service;
	}

	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}

	public OrtolangIndexableContent getContent() {
		return content;
	}

	public String getKey() {
		return key;
	}

}
