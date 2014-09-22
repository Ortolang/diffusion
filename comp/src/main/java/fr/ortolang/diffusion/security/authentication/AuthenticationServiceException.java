package fr.ortolang.diffusion.security.authentication;

@SuppressWarnings("serial")
public class AuthenticationServiceException extends Exception {

	public AuthenticationServiceException() {
		super();
	}

	public AuthenticationServiceException(String message) {
		super(message);
	}

	public AuthenticationServiceException(Throwable cause) {
		super(cause);
	}

	public AuthenticationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}