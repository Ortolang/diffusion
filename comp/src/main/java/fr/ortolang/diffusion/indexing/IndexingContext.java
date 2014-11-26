package fr.ortolang.diffusion.indexing;

public class IndexingContext {

	private String root;
	private String path;
	
	public IndexingContext(String root, String path) {
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
}
