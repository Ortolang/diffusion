package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLElement {

	private String name;
	private String prefixNamespace;
	private Map<String, String> attributes;
	private String value;

	public XMLElement() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefixNamespace() {
		return prefixNamespace;
	}

	public void setPrefixNamespace(String prefixNamespace) {
		this.prefixNamespace = prefixNamespace;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public static XMLElement createDcElement(String name, String value) {
		XMLElement elem = new XMLElement();
		elem.setPrefixNamespace("dc");
		elem.setName(name);
		elem.setValue(value);
		return elem;
	}
	
	public static XMLElement createDctermsElement(String name, String value) {
		XMLElement elem = new XMLElement();
		elem.setPrefixNamespace("dcterms");
		elem.setName(name);
		elem.setValue(value);
		return elem;
	}
	
	public XMLElement withAttribute(String name, String value) {
		if(this.attributes==null) {
			this.attributes = new HashMap<String, String>();
		}
		this.attributes.put(name, value);
		return this;
	}
}
