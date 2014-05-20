package fr.ortolang.diffusion.workflow;

@SuppressWarnings("serial")
public class WorkflowServiceException extends Exception {

	public WorkflowServiceException() {
		super();
	}

	public WorkflowServiceException(String message) {
		super(message);
	}

	public WorkflowServiceException(Throwable cause) {
		super(cause);
	}

	public WorkflowServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}