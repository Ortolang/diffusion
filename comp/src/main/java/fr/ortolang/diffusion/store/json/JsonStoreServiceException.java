package fr.ortolang.diffusion.store.json;

@SuppressWarnings("serial")
public class JsonStoreServiceException extends Exception {

	public JsonStoreServiceException() {
		super();
	}

	public JsonStoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public JsonStoreServiceException(String message) {
		super(message);
	}

	public JsonStoreServiceException(Throwable cause) {
		super(cause);
	}

}
