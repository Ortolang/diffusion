package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class TagNotFoundException extends Exception {

	public TagNotFoundException() {
		super();
	}

	public TagNotFoundException(String message) {
		super(message);
	}

	public TagNotFoundException(Throwable cause) {
		super(cause);
	}

	public TagNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public TagNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
