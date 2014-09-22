package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class InvalidPathException extends Exception {

	public InvalidPathException() {
		super();
	}

	public InvalidPathException(String message) {
		super(message);
	}

	public InvalidPathException(Throwable cause) {
		super(cause);
	}

	public InvalidPathException(String message, Throwable cause) {
		super(message, cause);
	}

}