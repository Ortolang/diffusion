package fr.ortolang.diffusion.core.entity;

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class SnapshotElement implements Serializable {
	
	private String name;
	private String key;

	public SnapshotElement() {
	}

	public SnapshotElement(String name, String key) {
		this.name = name;
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public SnapshotElement clone() {
		SnapshotElement clone = new SnapshotElement();
		clone.setKey(this.getKey());
		clone.setName(this.getName());
		return clone;
	}
	
	public String serialize() {
		return this.getName() + "/" + this.getKey();
	}
	
	public static SnapshotElement deserialize(String serializedElement) {
		if (serializedElement == null) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(serializedElement, "/");
		return new SnapshotElement(tokenizer.nextToken(), tokenizer.nextToken());
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{Name:").append(getName());
		buffer.append(",Key:").append(getKey()).append("}");
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SnapshotElement other = (SnapshotElement) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
