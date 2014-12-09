package fr.ortolang.diffusion.tool.invoke;

public class ToolJobInvocationResult {

	public enum Status {
		SUCCESS, ERROR
	}

	private String output;
	private Status status;
	
	public ToolJobInvocationResult() {
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
}
