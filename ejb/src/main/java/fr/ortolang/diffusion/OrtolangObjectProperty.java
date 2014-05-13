package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectProperty implements Serializable {

	public static final String SYSTEM_PROPERTY_PREFIX = "system.";
	public static final String CREATION_TIMESTAMP = SYSTEM_PROPERTY_PREFIX + "created";
	public static final String LAST_UPDATE_TIMESTAMP = SYSTEM_PROPERTY_PREFIX + "modified";
	public static final String PUBLICATION_TIMESTAMP = SYSTEM_PROPERTY_PREFIX + "published";
	public static final String AUTHOR = SYSTEM_PROPERTY_PREFIX + "author";
	
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

}
