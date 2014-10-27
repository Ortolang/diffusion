package fr.ortolang.diffusion.tool;

@SuppressWarnings("serial")
public class ToolServiceException extends Exception {

	public ToolServiceException() {
		super();
	}

	public ToolServiceException(String message) {
		super(message);
	}

	public ToolServiceException(Throwable cause) {
		super(cause);
	}

	public ToolServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}