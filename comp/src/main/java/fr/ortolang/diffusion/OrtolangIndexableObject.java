package fr.ortolang.diffusion;

import java.util.List;

import fr.ortolang.diffusion.indexing.IndexingContext;

public class OrtolangIndexableObject {

	private OrtolangObjectIdentifier identifier;
	private String service;
	private String type;
	private String key;
	private String name;
	private boolean locked;
	private boolean hidden;
	private String status;
	private String author;
	private long creationDate;
	private long lastModificationDate;
	private List<OrtolangObjectProperty> properties;
	private OrtolangIndexablePlainTextContent plainTextContent;
	private OrtolangIndexableSemanticContent semanticContent;
	private IndexingContext context;

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

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(long lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public IndexingContext getContext() {
		return context;
	}

	public void setContext(IndexingContext context) {
		this.context = context;
	}

}
