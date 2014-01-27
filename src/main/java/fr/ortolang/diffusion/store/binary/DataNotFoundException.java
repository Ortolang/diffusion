package fr.ortolang.diffusion.store.binary;

/**
 * An object with this hash does not exists in the storage. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataNotFoundException extends Exception {

	public DataNotFoundException() {
		super();
	}

	public DataNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataNotFoundException(String message) {
		super(message);
	}

	public DataNotFoundException(Throwable cause) {
		super(cause);
	}

}
