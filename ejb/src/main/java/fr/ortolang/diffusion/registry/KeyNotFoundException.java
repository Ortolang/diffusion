package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class KeyNotFoundException extends Exception {

	public KeyNotFoundException() {
		super();
	}

	public KeyNotFoundException(String message) {
		super(message);
	}

	public KeyNotFoundException(Throwable cause) {
		super(cause);
	}

	public KeyNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeyNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
