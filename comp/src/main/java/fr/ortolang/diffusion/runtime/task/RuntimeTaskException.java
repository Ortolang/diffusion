package fr.ortolang.diffusion.runtime.task;

@SuppressWarnings("serial")
public class RuntimeTaskException extends Exception {

	public RuntimeTaskException() {
	}

	public RuntimeTaskException(String message) {
		super(message);
	}

	public RuntimeTaskException(Throwable cause) {
		super(cause);
	}

	public RuntimeTaskException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeTaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
