package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class CollectionElementNotFoundException extends Exception {

	public CollectionElementNotFoundException() {
		super();
	}

	public CollectionElementNotFoundException(String message) {
		super(message);
	}

	public CollectionElementNotFoundException(Throwable cause) {
		super(cause);
	}

	public CollectionElementNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}