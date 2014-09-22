package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectInfos implements Serializable {

	private String author;
	private long creationDate;
	private long lastModificationDate;

	public OrtolangObjectInfos() {
	}

	public OrtolangObjectInfos(String author, long creationDate, long lastModificationDate) {
		this.author = author;
		this.creationDate = creationDate;
		this.lastModificationDate = lastModificationDate;
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

}