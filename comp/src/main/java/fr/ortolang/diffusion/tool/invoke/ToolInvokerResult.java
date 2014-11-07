package fr.ortolang.diffusion.tool.invoke;

import java.util.List;

public class ToolInvokerResult {

	public enum Status {
		SUCCESS, ERROR
	}

	private long start;
	private long stop;
	private String output;
	private List<String> outputFilePath;
	private String log;
	private Status status;
	
	public ToolInvokerResult() {
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getStop() {
		return stop;
	}

	public void setStop(long stop) {
		this.stop = stop;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public List<String> getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(List<String> outputFilePath) {
		this.outputFilePath = outputFilePath;
	}


}
