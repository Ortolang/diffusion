package fr.ortolang.diffusion.workflow.entity;

import org.activiti.engine.repository.ProcessDefinition;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.workflow.WorkflowService;

@SuppressWarnings("serial")
public class WorkflowDefinition extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "workflow-definition";

	private String key;
	private String id;
	private String name;
	private String friendlyName;
	private String description;
	private boolean suspended;
	private int version;

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
	
	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
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
		instance.setName(pdef.getKey());
		instance.setFriendlyName(pdef.getName());
		instance.setDescription(pdef.getDescription());
		instance.setVersion(pdef.getVersion());
		instance.setSuspended(pdef.isSuspended());
		return instance;
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, id);
	}

}
