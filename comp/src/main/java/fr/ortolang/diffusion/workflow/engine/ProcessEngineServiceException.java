package fr.ortolang.diffusion.workflow.engine;

@SuppressWarnings("serial")
public class ProcessEngineServiceException extends Exception {

	public ProcessEngineServiceException() {
		super();
	}

	public ProcessEngineServiceException(String message) {
		super(message);
	}

	public ProcessEngineServiceException(Throwable cause) {
		super(cause);
	}

	public ProcessEngineServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}