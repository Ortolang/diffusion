package fr.ortolang.diffusion.client.account;

@SuppressWarnings("serial")
public class OrtolangClientAccountException extends Exception {

	public OrtolangClientAccountException() {
	}

	public OrtolangClientAccountException(String message) {
		super(message);
	}

	public OrtolangClientAccountException(Throwable cause) {
		super(cause);
	}

	public OrtolangClientAccountException(String message, Throwable cause) {
		super(message, cause);
	}

	public OrtolangClientAccountException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
