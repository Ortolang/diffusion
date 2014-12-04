package fr.ortolang.diffusion.tool.job;

@SuppressWarnings("serial")
public class ToolJobException extends Exception {

	public ToolJobException() {
		super();
	}

	public ToolJobException(String message) {
		super(message);
	}

	public ToolJobException(Throwable cause) {
		super(cause);
	}

	public ToolJobException(String message, Throwable cause) {
		super(message, cause);
	}

}
