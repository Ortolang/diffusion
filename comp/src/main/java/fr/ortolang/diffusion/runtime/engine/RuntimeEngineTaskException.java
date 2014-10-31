package fr.ortolang.diffusion.runtime.engine;

@SuppressWarnings("serial")
public class RuntimeEngineTaskException extends Exception {

	public RuntimeEngineTaskException() {
	}

	public RuntimeEngineTaskException(String message) {
		super(message);
	}

	public RuntimeEngineTaskException(Throwable cause) {
		super(cause);
	}

	public RuntimeEngineTaskException(String message, Throwable cause) {
		super(message, cause);
	}

	public RuntimeEngineTaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
