package fr.ortolang.diffusion.core.entity;

import java.util.HashSet;
import java.util.Set;

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
public class Collection extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "collection";
	
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String description;
	@ElementCollection(fetch=FetchType.EAGER)
	private Set<String> elements;
	
	public Collection() {
		elements = new HashSet<String>();
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

	public void setElements(Set<String> elements) {
		this.elements = elements;
	}
	
	public Set<String> getElements() {
		return elements;
	}
	
	public boolean addElement(String element) {
		return this.elements.add(element);
	}
	
	public boolean removeElement(String element) {
		return this.elements.remove(element);
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
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, id);
	}
	
}
