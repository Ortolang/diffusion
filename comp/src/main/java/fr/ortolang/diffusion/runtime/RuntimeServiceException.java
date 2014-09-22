package fr.ortolang.diffusion.runtime;

@SuppressWarnings("serial")
public class RuntimeServiceException extends Exception {

	public RuntimeServiceException() {
		super();
	}

	public RuntimeServiceException(String message) {
		super(message);
	}

	public RuntimeServiceException(Throwable cause) {
		super(cause);
	}

	public RuntimeServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}