package fr.ortolang.diffusion.storage;

@SuppressWarnings("serial")
public class ObjectCorruptedException extends StorageServiceException {

	public ObjectCorruptedException() {
		super();
	}

	public ObjectCorruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObjectCorruptedException(String message) {
		super(message);
	}

	public ObjectCorruptedException(Throwable cause) {
		super(cause);
	}

}
