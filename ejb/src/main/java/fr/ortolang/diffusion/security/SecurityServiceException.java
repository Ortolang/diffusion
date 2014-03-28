package fr.ortolang.diffusion.security;

@SuppressWarnings("serial")
public class SecurityServiceException extends Exception {

	public SecurityServiceException(String message) {
		super(message);
	}

	public SecurityServiceException(Throwable cause) {
		super(cause);
	}

	public SecurityServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}