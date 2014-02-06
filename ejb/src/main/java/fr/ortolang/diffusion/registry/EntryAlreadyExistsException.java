package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class EntryAlreadyExistsException extends Exception {

	public EntryAlreadyExistsException() {
		super();
	}

	public EntryAlreadyExistsException(String message) {
		super(message);
	}

	public EntryAlreadyExistsException(Throwable cause) {
		super(cause);
	}

	public EntryAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public EntryAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
