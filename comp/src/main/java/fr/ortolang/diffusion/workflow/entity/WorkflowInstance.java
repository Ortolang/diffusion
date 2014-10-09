package fr.ortolang.diffusion.workflow.entity;

import java.util.Map;

import org.activiti.engine.runtime.ProcessInstance;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.workflow.WorkflowService;

@SuppressWarnings("serial")
public class WorkflowInstance extends OrtolangObject {

	public static final String OBJECT_TYPE = "workflow-instance";
	public static final String INITIER = "initier";

	private String key;
	private String id;
	private String name;
	private String definitionId;
	private String initier;
	private Map<String, Object> params;
	private boolean suspended;

	public WorkflowInstance() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}
	
	public String getDefinitionId() {
		return definitionId;
	}

	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}

	public static WorkflowInstance fromProcessInstance(ProcessInstance pins) {
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(pins.getBusinessKey());
		instance.setName(pins.getName());
		instance.setParams(pins.getProcessVariables());
		instance.setDefinitionId(pins.getProcessDefinitionId());
		instance.setInitier((String) pins.getProcessVariables().get(WorkflowInstance.INITIER));
		instance.setSuspended(pins.isSuspended());
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
		return new OrtolangObjectIdentifier(WorkflowService.SERVICE_NAME, WorkflowInstance.OBJECT_TYPE, id);
	}
}
