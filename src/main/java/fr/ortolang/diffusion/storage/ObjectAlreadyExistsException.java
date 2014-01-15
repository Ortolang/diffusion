package fr.ortolang.diffusion.storage;

/**
 * An object with the same content already exists in the storage. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ObjectAlreadyExistsException extends Exception {

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
