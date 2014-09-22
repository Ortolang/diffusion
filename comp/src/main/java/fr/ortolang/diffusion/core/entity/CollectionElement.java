package fr.ortolang.diffusion.core.entity;

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class CollectionElement implements Serializable {

	private String type;
	private String name;
	private long modification;
	private String key;

	public CollectionElement() {
	}

	public CollectionElement(String type, String name, long modification, String key) {
		this.type = type;
		this.name = name;
		this.modification = modification;
		this.key = key;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getModification() {
		return modification;
	}

	public void setModification(long modification) {
		this.modification = modification;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String serialize() {
		return this.getType() + "/" + this.getName() + "/" + this.getModification() + "/" + this.getKey();
	}

	public static CollectionElement deserialize(String serializedElement) {
		if (serializedElement == null) {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(serializedElement, "/");
		return new CollectionElement(tokenizer.nextToken(), tokenizer.nextToken(), Long.parseLong(tokenizer.nextToken()), tokenizer.nextToken());
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{Type:").append(getType());
		buffer.append(",Name:").append(getName());
		buffer.append(",Modification:").append(getModification());
		buffer.append(",Key:").append(getKey()).append("}");
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + (int) (modification ^ (modification >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		CollectionElement other = (CollectionElement) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (modification != other.modification)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

}
