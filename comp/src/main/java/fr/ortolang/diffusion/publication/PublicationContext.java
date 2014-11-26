package fr.ortolang.diffusion.publication;

import fr.ortolang.diffusion.publication.type.PublicationType;

public class PublicationContext {

	private PublicationType type;
	private String root;
	private String path;
	private String name;
	
	public PublicationContext(PublicationType type, String root, String path, String name) {
		this.type = type;
		this.root = root;
		this.path = path;
		this.name = name;
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

	public PublicationType getType() {
		return type;
	}

	public void setType(PublicationType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
