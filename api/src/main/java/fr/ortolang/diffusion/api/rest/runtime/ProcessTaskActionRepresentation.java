package fr.ortolang.diffusion.api.rest.runtime;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "workflow-task-action")
public class ProcessTaskActionRepresentation {

	private String action;
	private String assignee;
	private List<ProcessVariableRepresentation> variables;

	public ProcessTaskActionRepresentation() {
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public List<ProcessVariableRepresentation> getVariables() {
		return variables;
	}

	public void setVariables(List<ProcessVariableRepresentation> variables) {
		this.variables = variables;
	}

}
