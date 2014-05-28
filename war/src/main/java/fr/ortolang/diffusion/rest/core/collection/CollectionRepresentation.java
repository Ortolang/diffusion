package fr.ortolang.diffusion.rest.core.collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.rest.api.OrtolangLinkableRepresentation;

@XmlRootElement(name="collection")
public class CollectionRepresentation extends OrtolangLinkableRepresentation {

	@XmlAttribute(name="key")
	private String key;
	private String name;
	private String description;
	
	public CollectionRepresentation() {
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

	public static CollectionRepresentation fromCollection(Collection collection) {
		CollectionRepresentation representation = new CollectionRepresentation();
		representation.setKey(collection.getKey());
		representation.setName(collection.getName());
		representation.setDescription(collection.getDescription());
		return representation;
	}
}
