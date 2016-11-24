package fr.ortolang.diffusion.oai;

public class SetRecordNotFoundException extends Exception {

	public SetRecordNotFoundException() {
		super();
	}

	public SetRecordNotFoundException(String message) {
		super(message);
	}

	public SetRecordNotFoundException(Throwable cause) {
		super(cause);
	}

	public SetRecordNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public SetRecordNotFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
