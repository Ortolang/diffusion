package fr.ortolang.diffusion.store.binary;

/**
 * The current stored binary stream has generated a different hash that the one used for storage meaning 
 * that the stored data has been corrupted since original storage. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataCorruptedException extends Exception {

	public DataCorruptedException() {
		super();
	}

	public DataCorruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataCorruptedException(String message) {
		super(message);
	}

	public DataCorruptedException(Throwable cause) {
		super(cause);
	}

}
