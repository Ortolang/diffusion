package fr.ortolang.diffusion.security.authorisation;

@SuppressWarnings("serial")
public class AccessDeniedException extends Exception {

	public AccessDeniedException(String message) {
		super(message);
	}

	public AccessDeniedException(Throwable cause) {
		super(cause);
	}

	public AccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

}