package fr.ortolang.diffusion;

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class DiffusionObjectIdentifier implements Serializable {

	private String service;
	private String type;
	private String id;

	public DiffusionObjectIdentifier() {
	}

	public DiffusionObjectIdentifier(String service, String type, String id) {
		this.service = service;
		this.type = type;
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}
	
	public String getService() {
		return service;
	}

	public static DiffusionObjectIdentifier deserialize(String serializedIdentifier) {
		if (serializedIdentifier == null) {
			return null;
		}

		StringTokenizer tokenizer = new StringTokenizer(serializedIdentifier, "/");
		return new DiffusionObjectIdentifier(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
	}

	public String serialize() {
		return this.getService() + "/" + this.getType() + "/" + this.getId();
	}

	@Override
	public String toString() {
		return "Service:" + getService() + "; Type:" + getType() + "; Id:" + getId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((id == null) ? 0 : id.hashCode());
		result = (prime * result) + ((type == null) ? 0 : type.hashCode());
		result = (prime * result) + ((service == null) ? 0 : service.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		DiffusionObjectIdentifier other = (DiffusionObjectIdentifier) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		
		if (service == null) {
			if (other.service != null) {
				return false;
			}
		} else if (!service.equals(other.service)) {
			return false;
		}

		return true;
	}
}
