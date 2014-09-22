package fr.ortolang.diffusion.browser;

@SuppressWarnings("serial")
public class BrowserServiceException extends Exception {

	public BrowserServiceException() {
		super();
	}

	public BrowserServiceException(String message) {
		super(message);
	}

	public BrowserServiceException(Throwable cause) {
		super(cause);
	}

	public BrowserServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public BrowserServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}