package fr.ortolang.diffusion.storage;


/**
 * An object with the another content has generated an identifier that is already used by another object.
 * This should NEVER happen. 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ObjectCollisionException extends Exception {

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
