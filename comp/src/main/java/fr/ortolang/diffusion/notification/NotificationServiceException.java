package fr.ortolang.diffusion.notification;

@SuppressWarnings("serial")
public class NotificationServiceException extends Exception {

	public NotificationServiceException() {
		super();
	}

	public NotificationServiceException(String message) {
		super(message);
	}

	public NotificationServiceException(Throwable cause) {
		super(cause);
	}

	public NotificationServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotificationServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}