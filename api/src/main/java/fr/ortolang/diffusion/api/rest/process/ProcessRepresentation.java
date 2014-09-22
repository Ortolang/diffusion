package fr.ortolang.diffusion.api.rest.process;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.runtime.entity.ProcessInstance;

@XmlRootElement(name = "process")
public class ProcessRepresentation {

	@XmlAttribute(name = "key")
	private String key;
	private String name;
	private String type;
	private String initier;
	private String state;
	private String step;
	private Map<String, String> params;
	private String log;

	public ProcessRepresentation() {
		params = new HashMap<String, String>();
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

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
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

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public static ProcessRepresentation fromProcess(ProcessInstance process) {
		ProcessRepresentation representation = new ProcessRepresentation();
		representation.setKey(process.getKey());
		representation.setName(process.getName());
		representation.setType(process.getType());
		representation.setInitier(process.getInitier());
		representation.setState(process.getState().name());
		representation.setStep(process.getCurrentStep() + "");
		representation.setParams(process.getParams());
		representation.setLog(process.getLog());
		return representation;
	}

}
