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

	public CoreServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}