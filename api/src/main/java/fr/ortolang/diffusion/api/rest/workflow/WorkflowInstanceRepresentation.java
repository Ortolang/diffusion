package fr.ortolang.diffusion.api.rest.workflow;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;

@XmlRootElement(name = "workflow-instance")
public class WorkflowInstanceRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String initier;
	private String activity;
	private boolean suspended;
	
	public WorkflowInstanceRepresentation() {
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

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}
	
	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public static WorkflowInstanceRepresentation fromWorkflowDefinition(WorkflowInstance instance) {
		WorkflowInstanceRepresentation representation = new WorkflowInstanceRepresentation();
		representation.setKey(instance.getKey());
		representation.setName(instance.getName());
		representation.setInitier(instance.getInitier());
		representation.setSuspended(instance.isSuspended());
		return representation;
	}

}