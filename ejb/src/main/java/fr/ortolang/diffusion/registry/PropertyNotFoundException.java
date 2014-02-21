package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class PropertyNotFoundException extends Exception {

	public PropertyNotFoundException() {
		super();
	}

	public PropertyNotFoundException(String message) {
		super(message);
	}

	public PropertyNotFoundException(Throwable cause) {
		super(cause);
	}

	public PropertyNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public PropertyNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
