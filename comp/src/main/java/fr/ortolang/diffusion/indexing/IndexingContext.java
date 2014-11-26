package fr.ortolang.diffusion.indexing;

public class IndexingContext {

	private String root;
	private String path;
	private String name;
	
	public IndexingContext(String root, String path, String name) {
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
