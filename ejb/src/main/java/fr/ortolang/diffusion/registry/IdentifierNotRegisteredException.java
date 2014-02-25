package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class IdentifierNotRegisteredException extends Exception {

	public IdentifierNotRegisteredException() {
		super();
	}

	public IdentifierNotRegisteredException(String message) {
		super(message);
	}

	public IdentifierNotRegisteredException(Throwable cause) {
		super(cause);
	}

	public IdentifierNotRegisteredException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdentifierNotRegisteredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
