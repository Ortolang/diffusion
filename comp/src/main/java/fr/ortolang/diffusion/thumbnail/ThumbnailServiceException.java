package fr.ortolang.diffusion.thumbnail;

@SuppressWarnings("serial")
public class ThumbnailServiceException extends Exception {

	public ThumbnailServiceException() {
	}

	public ThumbnailServiceException(String message) {
		super(message);
	}

	public ThumbnailServiceException(Throwable cause) {
		super(cause);
	}

	public ThumbnailServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ThumbnailServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
