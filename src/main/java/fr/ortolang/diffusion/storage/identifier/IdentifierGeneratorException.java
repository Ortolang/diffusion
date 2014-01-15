package fr.ortolang.diffusion.storage.identifier;

/**
 * The IdentifierGenerator is not able to generate an identifier for an internal reason.  
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class IdentifierGeneratorException extends Exception {

	public IdentifierGeneratorException() {
		super();
	}

	public IdentifierGeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	public IdentifierGeneratorException(String message) {
		super(message);
	}

	public IdentifierGeneratorException(Throwable cause) {
		super(cause);
	}
}
