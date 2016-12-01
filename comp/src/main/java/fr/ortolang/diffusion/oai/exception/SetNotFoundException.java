package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class SetNotFoundException extends Exception {

	public SetNotFoundException() {
		super();
	}

	public SetNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public SetNotFoundException(String message) {
		super(message);
	}

	public SetNotFoundException(Throwable cause) {
		super(cause);
	}

}
