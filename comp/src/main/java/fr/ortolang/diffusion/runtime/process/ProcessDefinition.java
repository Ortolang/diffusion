package fr.ortolang.diffusion.runtime.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ProcessDefinition implements Serializable {

	private String name;
	private String description;
	private List<ProcessStep> steps;
	
	public ProcessDefinition() {
		steps = new ArrayList<ProcessStep> ();
	}
	
	public ProcessDefinition(String name, String description, List<ProcessStep> steps) {
		this.name = name;
		this.description = description;
		this.steps = steps;
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

	public List<ProcessStep> getSteps() {
		return steps;
	}

	public void setSteps(List<ProcessStep> steps) {
		this.steps = steps;
	}

	public void addStep(ProcessStep step) {
		this.steps.add(step);
	}
	
	public ProcessStep getStep(int current) {
		if (steps.size() > current) {
			return steps.get(current);
		} else {
			return null;
		}
	}
}
