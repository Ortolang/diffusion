package fr.ortolang.diffusion.rest.core.collection;

import java.util.List;

import fr.ortolang.diffusion.core.entity.DigitalCollection;

public class DigitalCollectionRepresentation {

	private String key;
	private String name;
	private String description;
	private List<String> elements;
	
	public DigitalCollectionRepresentation() {
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
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getElements() {
		return this.elements;
	}
	
	public void setElements(List<String> elements) {
		this.elements = elements;
	}

	public static DigitalCollectionRepresentation fromDigitalCollection(DigitalCollection collection) {
		DigitalCollectionRepresentation representation = new DigitalCollectionRepresentation();
		representation.setKey(collection.getKey());
		representation.setName(collection.getName());
		representation.setDescription(collection.getDescription());
		representation.setElements(collection.getElements());
		return representation;
	}
}
