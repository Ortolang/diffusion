package fr.ortolang.diffusion.store.binary;


/**
 * Two different binary streams has generated the same hash. 
 * This should NEVER happen. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DataCollisionException extends Exception {

	public DataCollisionException() {
		super();
	}

	public DataCollisionException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataCollisionException(String message) {
		super(message);
	}

	public DataCollisionException(Throwable cause) {
		super(cause);
	}
}
