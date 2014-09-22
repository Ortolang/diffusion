package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class CloneException extends Exception {

	public CloneException() {
		super();
	}

	public CloneException(String message) {
		super(message);
	}

	public CloneException(Throwable cause) {
		super(cause);
	}

	public CloneException(String message, Throwable cause) {
		super(message, cause);
	}

}