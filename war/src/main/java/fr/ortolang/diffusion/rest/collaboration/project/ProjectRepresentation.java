package fr.ortolang.diffusion.rest.collaboration.project;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.collaboration.entity.Project;
import fr.ortolang.diffusion.rest.api.OrtolangLinkableRepresentation;

@XmlRootElement(name="project")
public class ProjectRepresentation extends OrtolangLinkableRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String type;
	
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

	public static ProjectRepresentation fromProject(Project project) {
		ProjectRepresentation representation = new ProjectRepresentation();
		representation.setKey(project.getKey());
		representation.setName(project.getName());
		representation.setType(project.getType());
		return representation;
	}

}
