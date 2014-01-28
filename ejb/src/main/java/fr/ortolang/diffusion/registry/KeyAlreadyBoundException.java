package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class KeyAlreadyBoundException extends Exception {

	public KeyAlreadyBoundException() {
		super();
	}

	public KeyAlreadyBoundException(String message) {
		super(message);
	}

	public KeyAlreadyBoundException(Throwable cause) {
		super(cause);
	}

	public KeyAlreadyBoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeyAlreadyBoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
