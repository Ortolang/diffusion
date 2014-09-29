package fr.ortolang.diffusion.workflow.entity;

import java.io.Serializable;

import org.activiti.engine.repository.ProcessDefinition;

@SuppressWarnings("serial")
public class WorkflowDefinition implements Serializable {

	public String id;
	public String key;
	public String name;
	public String description;
	public boolean suspended;
	public int version;

	public WorkflowDefinition() {
		suspended = false;
		version = -1;
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
	
	public static WorkflowDefinition fromProcessDefinition(ProcessDefinition pdef) {
		WorkflowDefinition instance = new WorkflowDefinition();
		instance.setId(pdef.getId());
		instance.setKey(pdef.getKey());
		instance.setName(pdef.getName());
		instance.setDescription(pdef.getDescription());
		instance.setVersion(pdef.getVersion());
		instance.setSuspended(pdef.isSuspended());
		return instance;
	}

}
