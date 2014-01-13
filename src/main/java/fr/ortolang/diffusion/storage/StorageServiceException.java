package fr.ortolang.diffusion.storage;

@SuppressWarnings("serial")
public class StorageServiceException extends Exception {

	public StorageServiceException() {
		super();
	}

	public StorageServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public StorageServiceException(String message) {
		super(message);
	}

	public StorageServiceException(Throwable cause) {
		super(cause);
	}

}
