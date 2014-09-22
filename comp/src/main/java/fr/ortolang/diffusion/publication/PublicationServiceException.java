package fr.ortolang.diffusion.publication;

@SuppressWarnings("serial")
public class PublicationServiceException extends Exception {

	public PublicationServiceException() {
		super();
	}

	public PublicationServiceException(String message) {
		super(message);
	}

	public PublicationServiceException(Throwable cause) {
		super(cause);
	}

	public PublicationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}