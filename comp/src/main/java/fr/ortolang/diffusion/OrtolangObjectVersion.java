package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectVersion implements Serializable {

	private String key;
	private String author;
	private long date;
	private String parent;
	private String children;
	
	public OrtolangObjectVersion() {
	}
	
	public OrtolangObjectVersion(String key, String author, long date, String parent, String children) {
		this.key = key;
		this.author = author;
		this.date = date;
		this.parent = parent;
		this.children = children;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getChildren() {
		return children;
	}

	public void setChildren(String children) {
		this.children = children;
	}

}
