package fr.ortolang.diffusion.share;

@SuppressWarnings("serial")
public class ShareServiceException extends Exception {

	public ShareServiceException() {
		super();
	}

	public ShareServiceException(String message) {
		super(message);
	}

	public ShareServiceException(Throwable cause) {
		super(cause);
	}

	public ShareServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}