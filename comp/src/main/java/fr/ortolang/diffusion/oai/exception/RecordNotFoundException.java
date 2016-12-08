package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class RecordNotFoundException extends Exception {

	public RecordNotFoundException() {
		super();
	}

	public RecordNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public RecordNotFoundException(String message) {
		super(message);
	}

	public RecordNotFoundException(Throwable cause) {
		super(cause);
	}

}
