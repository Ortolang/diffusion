package fr.ortolang.diffusion.rest.workflow.process;

import fr.ortolang.diffusion.rest.DiffusionRepresentation;
import fr.ortolang.diffusion.workflow.entity.Process;

public class ProcessRepresentation extends DiffusionRepresentation {

	private String key;
	private String name;
	private String type;
	private String initier;
	private String status;
	private String start;
	private String stop;
	private String log;

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

	public String getStatus() {
		return status;
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

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getStop() {
		return stop;
	}

	public void setStop(String stop) {
		this.stop = stop;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public static ProcessRepresentation fromProcess(Process process) {
		ProcessRepresentation representation = new ProcessRepresentation();
		representation.setKey(process.getKey());
		representation.setName(process.getName());
		representation.setType(process.getType());
		representation.setInitier(process.getInitier());
		representation.setStatus(process.getStatus());
		representation.setStart(process.getStart());
		representation.setStop(process.getStop());
		representation.setLog(process.getLog());
		return representation;
	}

}
