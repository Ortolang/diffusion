package fr.ortolang.diffusion.rest.collaboration.project;

import fr.ortolang.diffusion.collaboration.entity.Project;

public class ProjectRepresentation {

	private String key;
	private String name;
	private String type;
	private String category;

	public ProjectRepresentation() {
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public static ProjectRepresentation fromProject(Project project) {
		ProjectRepresentation representation = new ProjectRepresentation();
		representation.setKey(project.getKey());
		representation.setName(project.getName());
		representation.setType(project.getType());
		representation.setCategory(project.getCategory());
		return representation;
	}

}
