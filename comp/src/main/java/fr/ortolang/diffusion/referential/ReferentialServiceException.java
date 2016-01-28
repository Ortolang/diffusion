package fr.ortolang.diffusion.referential;

@SuppressWarnings("serial")
public class ReferentialServiceException extends Exception {

	public ReferentialServiceException() {
		super();
	}

	public ReferentialServiceException(String message) {
		super(message);
	}

	public ReferentialServiceException(Throwable cause) {
		super(cause);
	}

	public ReferentialServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
