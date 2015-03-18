package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class CollectionNotEmptyException extends Exception {

	public CollectionNotEmptyException() {
		super();
	}

	public CollectionNotEmptyException(String message) {
		super(message);
	}

	public CollectionNotEmptyException(Throwable cause) {
		super(cause);
	}

	public CollectionNotEmptyException(String message, Throwable cause) {
		super(message, cause);
	}

}