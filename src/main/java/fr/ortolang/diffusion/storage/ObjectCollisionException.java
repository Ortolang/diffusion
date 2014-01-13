package fr.ortolang.diffusion.storage;


@SuppressWarnings("serial")
public class ObjectCollisionException extends StorageServiceException {

	public ObjectCollisionException() {
		super();
	}

	public ObjectCollisionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectCollisionException(String message) {
		super(message);
	}

	public ObjectCollisionException(Throwable cause) {
		super(cause);
	}
}
