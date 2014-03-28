package fr.ortolang.diffusion.security.authorisation;

@SuppressWarnings("serial")
public class AuthorisationServiceException extends Exception {

	public AuthorisationServiceException(String message) {
		super(message);
	}

	public AuthorisationServiceException(Throwable cause) {
		super(cause);
	}

	public AuthorisationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}