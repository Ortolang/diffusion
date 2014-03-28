package fr.ortolang.diffusion.collaboration;


@SuppressWarnings("serial")
public class CollaborationServiceException extends Exception {

	public CollaborationServiceException() {
		super();
	}

	public CollaborationServiceException(String message) {
		super(message);
	}

	public CollaborationServiceException(Throwable cause) {
		super(cause);
	}

	public CollaborationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}