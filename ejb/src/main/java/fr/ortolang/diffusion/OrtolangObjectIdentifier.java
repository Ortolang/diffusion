package fr.ortolang.diffusion;

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class OrtolangObjectIdentifier implements Serializable {

	private String service;
	private String type;
	private String id;

	public OrtolangObjectIdentifier() {
	}

	public OrtolangObjectIdentifier(String service, String type, String id) {
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

	public static OrtolangObjectIdentifier deserialize(String serializedIdentifier) {
		if (serializedIdentifier == null) {
			return null;
		}

		StringTokenizer tokenizer = new StringTokenizer(serializedIdentifier, "/");
		return new OrtolangObjectIdentifier(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
	}
	
	public static String buildFilterPattern(String service, String type) {
		StringBuffer pattern = new StringBuffer();
		pattern.append("/");
		if ( service != null && service.length() > 0 ) {
			pattern.append(service.toLowerCase());
		} else {
			pattern.append(".*");
		}
		pattern.append("/");
		if ( type != null && type.length() > 0 ) {
			pattern.append(type.toLowerCase());
		} else {
			pattern.append(".*");
		}
		pattern.append("/.*");
		return pattern.toString();
	}

	public String serialize() {
		return "/" + this.getService() + "/" + this.getType() + "/" + this.getId();
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

		OrtolangObjectIdentifier other = (OrtolangObjectIdentifier) obj;

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
