package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangSearchResult implements Serializable {

	private String key;
	private float score;
	private String explain;
	private String name;
	private String service;
	private String type;
	private OrtolangObjectIdentifier identifier;
	private String root;
	private String path;

	public OrtolangSearchResult() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public String getExplain() {
		return explain;
	}

	public void setExplain(String explain) {
		this.explain = explain;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public OrtolangObjectIdentifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(OrtolangObjectIdentifier identifier) {
		this.identifier = identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = OrtolangObjectIdentifier.deserialize(identifier);
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[").append(getScore()).append("]");
		buffer.append(" key:").append(getKey());
		buffer.append(" service:").append(getService());
		buffer.append(" type:").append(getType());
		buffer.append(" name:").append(getName());
		buffer.append(" explain:").append(getExplain());
		return buffer.toString();
	}

}
