package fr.ortolang.diffusion.core.entity;

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class TagElement implements Serializable {

	private String name;
	private String snapshot;

	public TagElement() {
	}

	public TagElement(String name, String snapshot) {
		this.name = name;
		this.snapshot = snapshot;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}

	@Override
	public TagElement clone() {
		TagElement clone = new TagElement();
		clone.setName(this.getName());
		clone.setSnapshot(this.getSnapshot());
		return clone;
	}

	public String serialize() {
		return this.getName() + "/" + this.getSnapshot();
	}

	public static TagElement deserialize(String serializedElement) {
		if (serializedElement == null) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(serializedElement, "/");
		return new TagElement(tokenizer.nextToken(), tokenizer.nextToken());
	}

	@Override
	public String toString() {
		return "TagElement{" + "name='" + name + '\'' + ", snapshot='" + snapshot + '\'' + '}';
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((snapshot == null) ? 0 : snapshot.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagElement other = (TagElement) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (snapshot == null) {
			if (other.snapshot != null)
				return false;
		} else if (!snapshot.equals(other.snapshot))
			return false;
		return true;
	}

}
