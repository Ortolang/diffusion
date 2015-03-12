package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class KeyLockedException extends Exception {

	public KeyLockedException() {
		super();
	}

	public KeyLockedException(String message) {
		super(message);
	}

	public KeyLockedException(Throwable cause) {
		super(cause);
	}

	public KeyLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public KeyLockedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
