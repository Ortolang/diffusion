package fr.ortolang.diffusion.preview.generator;

@SuppressWarnings("serial")
public class PreviewGeneratorException extends Exception {

	public PreviewGeneratorException() {
	}

	public PreviewGeneratorException(String message) {
		super(message);
	}

	public PreviewGeneratorException(Throwable cause) {
		super(cause);
	}

	public PreviewGeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	public PreviewGeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
