package fr.ortolang.diffusion.workflow.entity;

import org.activiti.engine.runtime.ProcessInstance;

public class WorkflowInstance {

	private String id;
	private String name;
	private String parentId;
	private String activityId;
	private String deploymentId;
	private String initier;
	private String definitionId;
	private String definitionKey;
	private boolean suspended;

	public WorkflowInstance() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public String getDefinitionId() {
		return definitionId;
	}

	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}

	public String getDefinitionKey() {
		return definitionKey;
	}

	public void setDefinitionKey(String definitionKey) {
		this.definitionKey = definitionKey;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public static WorkflowInstance fromProcessInstance(ProcessInstance pins) {
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(pins.getId());
		instance.setName(pins.getName());
		instance.setParentId(pins.getParentId());
		instance.setActivityId(pins.getActivityId());
		instance.setDeploymentId(pins.getDeploymentId());
		instance.setInitier(pins.getTenantId());
		instance.setDefinitionId(pins.getProcessDefinitionId());
		instance.setDefinitionKey(pins.getProcessDefinitionKey());
		instance.setSuspended(pins.isSuspended());
		return instance;
	}
}
