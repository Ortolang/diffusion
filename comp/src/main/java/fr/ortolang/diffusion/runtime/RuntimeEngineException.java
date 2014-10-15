package fr.ortolang.diffusion.runtime;

@SuppressWarnings("serial")
public class RuntimeEngineException extends Exception {

	public RuntimeEngineException() {
		super();
	}

	public RuntimeEngineException(String message) {
		super(message);
	}

	public RuntimeEngineException(Throwable cause) {
		super(cause);
	}

	public RuntimeEngineException(String message, Throwable cause) {
		super(message, cause);
	}

}