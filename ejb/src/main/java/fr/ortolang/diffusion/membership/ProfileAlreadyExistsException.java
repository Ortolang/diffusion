package fr.ortolang.diffusion.membership;

@SuppressWarnings("serial")
public class ProfileAlreadyExistsException extends Exception {

	public ProfileAlreadyExistsException() {
		super();
	}

	public ProfileAlreadyExistsException(String message) {
		super(message);
	}

	public ProfileAlreadyExistsException(Throwable cause) {
		super(cause);
	}

	public ProfileAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProfileAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
