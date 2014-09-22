package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class TreeBuilderException extends Exception {

	public TreeBuilderException() {
		super();
	}

	public TreeBuilderException(String message) {
		super(message);
	}

	public TreeBuilderException(Throwable cause) {
		super(cause);
	}

	public TreeBuilderException(String message, Throwable cause) {
		super(message, cause);
	}

}