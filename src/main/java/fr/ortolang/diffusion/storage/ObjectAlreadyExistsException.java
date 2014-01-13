package fr.ortolang.diffusion.storage;

@SuppressWarnings("serial")
public class ObjectAlreadyExistsException extends StorageServiceException {

	public ObjectAlreadyExistsException() {
		super();
	}

	public ObjectAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectAlreadyExistsException(String message) {
		super(message);
	}

	public ObjectAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
