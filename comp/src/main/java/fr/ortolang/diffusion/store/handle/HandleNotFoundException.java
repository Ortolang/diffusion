package fr.ortolang.diffusion.store.handle;

@SuppressWarnings("serial")
public class HandleNotFoundException extends Exception {

	public HandleNotFoundException() {
		super();
	}

	public HandleNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public HandleNotFoundException(String message) {
		super(message);
	}

	public HandleNotFoundException(Throwable cause) {
		super(cause);
	}

}
