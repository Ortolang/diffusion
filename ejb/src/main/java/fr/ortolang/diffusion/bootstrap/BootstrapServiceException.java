package fr.ortolang.diffusion.bootstrap;

@SuppressWarnings("serial")
public class BootstrapServiceException extends Exception {

	public BootstrapServiceException() {
		super();
	}

	public BootstrapServiceException(String message) {
		super(message);
	}

	public BootstrapServiceException(Throwable cause) {
		super(cause);
	}

	public BootstrapServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public BootstrapServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}