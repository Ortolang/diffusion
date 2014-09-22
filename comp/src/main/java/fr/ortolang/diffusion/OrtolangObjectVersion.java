package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectVersion implements Serializable {

	private String key;
	private String name;
	private String author;
	private String date;
	private String parent;
	private String children;
	
	public OrtolangObjectVersion() {
	}
	
	public OrtolangObjectVersion(String key, String name, String author, String date, String parent, String children) {
		this.key = key;
		this.name = name;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
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
