package fr.ortolang.diffusion.storage;

/**
 * The object's content in the storage has generated another identifier that the one used for the storage.
 * It seems that the stored data has been corrupted since original storage. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ObjectCorruptedException extends Exception {

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
