package fr.ortolang.diffusion;

import java.util.List;

public class OrtolangIndexableObject {

	private OrtolangObjectIdentifier identifier;
	private String service;
	private String type;
	private String key;
	private String name;
	private boolean locked;
	private boolean deleted;
	private boolean hidden;
	private String status;
	private List<OrtolangObjectProperty> properties;
	private OrtolangIndexablePlainTextContent plainTextContent;
	private OrtolangIndexableSemanticContent semanticContent;
	
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

	public void setPlainTextContent(OrtolangIndexablePlainTextContent content) {
		this.plainTextContent = content;
	}
	
	public void setSemanticContent(OrtolangIndexableSemanticContent content) {
		this.semanticContent = content;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public void setProperties(List<OrtolangObjectProperty> properties) {
		this.properties = properties;
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

	public OrtolangIndexablePlainTextContent getPlainTextContent() {
		return plainTextContent;
	}
	
	public OrtolangIndexableSemanticContent getSemanticContent() {
		return semanticContent;
	}

	public String getKey() {
		return key;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public String getStatus() {
		return status;
	}
	
	public List<OrtolangObjectProperty> getProperties() {
		return properties;
	}
	
}
