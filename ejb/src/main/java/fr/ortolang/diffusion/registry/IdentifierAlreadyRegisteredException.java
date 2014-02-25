package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class IdentifierAlreadyRegisteredException extends Exception {

	public IdentifierAlreadyRegisteredException() {
		super();
	}

	public IdentifierAlreadyRegisteredException(String message) {
		super(message);
	}

	public IdentifierAlreadyRegisteredException(Throwable cause) {
		super(cause);
	}

	public IdentifierAlreadyRegisteredException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdentifierAlreadyRegisteredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
