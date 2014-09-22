package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectProperty implements Serializable {

	public static final String SYSTEM_PROPERTY_PREFIX = "system.";
	public static final String COLLECTION_PROPERTY = "isCollection";
		
	private String name;
	private String value;

	public OrtolangObjectProperty() {
	}

	public OrtolangObjectProperty(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{Name:").append(getName());
		buffer.append(",Value:").append(getValue()).append("}");
		return buffer.toString();
	}

}
