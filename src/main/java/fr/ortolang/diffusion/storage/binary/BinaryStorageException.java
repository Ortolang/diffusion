package fr.ortolang.diffusion.storage.binary;

/**
 * Binary Storage Exception is raised when a storage sub system problem occurs. This type of exception is not supposed to
 * happen and can wrap any kind of storage sub system error.
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class BinaryStorageException extends Exception {

	public BinaryStorageException() {
		super();
	}

	public BinaryStorageException(String message, Throwable cause) {
		super(message, cause);
	}

	public BinaryStorageException(String message) {
		super(message);
	}

	public BinaryStorageException(Throwable cause) {
		super(cause);
	}

}
