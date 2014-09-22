package fr.ortolang.diffusion.store.handle;

@SuppressWarnings("serial")
public class HandleStoreServiceException extends Exception {

	public HandleStoreServiceException() {
		super();
	}

	public HandleStoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public HandleStoreServiceException(String message) {
		super(message);
	}

	public HandleStoreServiceException(Throwable cause) {
		super(cause);
	}

}