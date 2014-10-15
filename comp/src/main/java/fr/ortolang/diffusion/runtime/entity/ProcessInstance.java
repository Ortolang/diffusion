package fr.ortolang.diffusion.runtime.entity;

import java.util.Map;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.runtime.RuntimeService;

@SuppressWarnings("serial")
public class ProcessInstance extends OrtolangObject {

	public static final String OBJECT_TYPE = "process-instance";
	public static final String INITIER = "initier";

	private String key;
	private String id;
	private String name;
	private String activityId;
	private String definitionId;
	private String initier;
	private Map<String, Object> variables;
	private boolean suspended;

	public ProcessInstance() {
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
		return variables;
	}

	public void setParams(Map<String, Object> params) {
		this.variables = params;
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

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public String getDefinitionId() {
		return definitionId;
	}

	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
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
		return new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, id);
	}
}
