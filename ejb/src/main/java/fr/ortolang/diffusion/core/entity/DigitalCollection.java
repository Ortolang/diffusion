package fr.ortolang.diffusion.core.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@SuppressWarnings("serial")
public class DigitalCollection extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "collection";
	
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String description;
	@ElementCollection(fetch=FetchType.EAGER)
	private List<String> elements;
	
	public DigitalCollection() {
		elements = new ArrayList<String>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public void setElements(List<String> elements) {
		this.elements = elements;
	}
	
	public List<String> getElements() {
		return elements;
	}
	
	public void addElement(String element) {
		this.elements.add(element);
	}
	
	public void removeElement(String element) {
		this.elements.remove(element);
	}

	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, id);
	}
	
}
