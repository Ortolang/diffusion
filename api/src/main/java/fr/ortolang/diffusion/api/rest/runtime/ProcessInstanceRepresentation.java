package fr.ortolang.diffusion.api.rest.runtime;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.runtime.entity.ProcessInstance;

@XmlRootElement(name = "workflow-instance")
public class ProcessInstanceRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String initier;
	private String activity;
	private boolean suspended;
	
	public ProcessInstanceRepresentation() {
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

	public static ProcessInstanceRepresentation fromProcessDefinition(ProcessInstance instance) {
		ProcessInstanceRepresentation representation = new ProcessInstanceRepresentation();
		representation.setKey(instance.getKey());
		representation.setName(instance.getName());
		representation.setInitier(instance.getInitier());
		representation.setActivity(instance.getActivityId());
		representation.setSuspended(instance.isSuspended());
		return representation;
	}

}