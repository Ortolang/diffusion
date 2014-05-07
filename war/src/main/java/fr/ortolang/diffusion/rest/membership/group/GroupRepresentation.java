package fr.ortolang.diffusion.rest.membership.group;

import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.rest.DiffusionRepresentation;

public class GroupRepresentation extends DiffusionRepresentation {
	
	private String key;
	private String name;
	private String description;
	
	public GroupRepresentation() {
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

	public static GroupRepresentation fromGroup (Group group) {
		GroupRepresentation representation = new GroupRepresentation();
		representation.setKey(group.getKey());
		representation.setName(group.getName());
		representation.setDescription(group.getDescription());
		return representation;
	}
	
}
