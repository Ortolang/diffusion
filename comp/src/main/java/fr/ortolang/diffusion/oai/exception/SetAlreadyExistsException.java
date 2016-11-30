package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class SetAlreadyExistsException extends Exception {

	public SetAlreadyExistsException() {
		super();
	}

	public SetAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public SetAlreadyExistsException(String message) {
		super(message);
	}

	public SetAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
