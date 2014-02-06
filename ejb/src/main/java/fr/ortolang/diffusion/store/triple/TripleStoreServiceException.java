package fr.ortolang.diffusion.store.triple;

@SuppressWarnings("serial")
public class TripleStoreServiceException extends Exception {

	public TripleStoreServiceException() {
		super();
	}

	public TripleStoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public TripleStoreServiceException(String message) {
		super(message);
	}

	public TripleStoreServiceException(Throwable cause) {
		super(cause);
	}

}