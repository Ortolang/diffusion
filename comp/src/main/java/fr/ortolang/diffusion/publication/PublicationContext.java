package fr.ortolang.diffusion.publication;

import fr.ortolang.diffusion.publication.type.PublicationType;

public class PublicationContext {

	private PublicationType type;
	private String root;
	private String path;
	
	public PublicationContext(PublicationType type, String root, String path) {
		this.type = type;
		this.root = root;
		this.path = path;
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
}
