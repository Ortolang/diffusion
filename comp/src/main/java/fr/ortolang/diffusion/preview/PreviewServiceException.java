package fr.ortolang.diffusion.preview;

@SuppressWarnings("serial")
public class PreviewServiceException extends Exception {

	public PreviewServiceException() {
	}

	public PreviewServiceException(String message) {
		super(message);
	}

	public PreviewServiceException(Throwable cause) {
		super(cause);
	}

	public PreviewServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PreviewServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
