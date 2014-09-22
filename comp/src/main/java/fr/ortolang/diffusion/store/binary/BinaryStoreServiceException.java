package fr.ortolang.diffusion.store.binary;

/**
 * BinaryStoreServiceException is raised when a storage sub system problem occurs. This type of exception is not supposed to
 * happen and can wrap any kind of storage sub system error.
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class BinaryStoreServiceException extends Exception {

	public BinaryStoreServiceException() {
		super();
	}

	public BinaryStoreServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public BinaryStoreServiceException(String message) {
		super(message);
	}

	public BinaryStoreServiceException(Throwable cause) {
		super(cause);
	}

}
