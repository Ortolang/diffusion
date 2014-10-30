package fr.ortolang.diffusion.api.rest.runtime;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.runtime.entity.Process;

@XmlRootElement(name = "process")
public class ProcessRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String initier;
	private String type;
	private String log;
	private String state;
	private String activity;
	
	public ProcessRepresentation() {
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

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public static ProcessRepresentation fromProcess(Process instance) {
		ProcessRepresentation representation = new ProcessRepresentation();
		representation.setKey(instance.getKey());
		representation.setName(instance.getName());
		representation.setInitier(instance.getInitier());
		representation.setLog(instance.getLog());
		representation.setState(instance.getState().name());
		representation.setActivity(instance.getActivity());
		representation.setType(instance.getType());
		return representation;
	}

}