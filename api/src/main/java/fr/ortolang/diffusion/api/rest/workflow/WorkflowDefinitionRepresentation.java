package fr.ortolang.diffusion.api.rest.workflow;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;

@XmlRootElement(name = "workflow-definition")
public class WorkflowDefinitionRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String friendlyName;
	private String description;
	private boolean suspended;
	private int version;

	public WorkflowDefinitionRepresentation() {
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

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public static WorkflowDefinitionRepresentation fromWorkflowDefinition(WorkflowDefinition definition) {
		WorkflowDefinitionRepresentation representation = new WorkflowDefinitionRepresentation();
		representation.setKey(definition.getKey());
		representation.setName(definition.getName());
		representation.setFriendlyName(definition.getFriendlyName());
		representation.setDescription(definition.getDescription());
		representation.setSuspended(definition.isSuspended());
		representation.setVersion(definition.getVersion());
		return representation;
	}

}
