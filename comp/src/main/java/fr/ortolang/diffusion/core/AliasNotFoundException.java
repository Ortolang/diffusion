package fr.ortolang.diffusion.core;


@SuppressWarnings("serial")
public class AliasNotFoundException extends Exception {

	public AliasNotFoundException(String message) {
		super(message);
	}

	public AliasNotFoundException(Throwable cause) {
		super(cause);
	}

	public AliasNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public AliasNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
