package fr.ortolang.diffusion.core;


@SuppressWarnings("serial")
public class CoreServiceException extends Exception {

	public CoreServiceException() {
		super();
	}

	public CoreServiceException(String message) {
		super(message);
	}

	public CoreServiceException(Throwable cause) {
		super(cause);
	}

	public CoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}