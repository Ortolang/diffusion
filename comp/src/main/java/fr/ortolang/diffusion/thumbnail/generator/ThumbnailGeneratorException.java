package fr.ortolang.diffusion.thumbnail.generator;

@SuppressWarnings("serial")
public class ThumbnailGeneratorException extends Exception {

	public ThumbnailGeneratorException() {
	}

	public ThumbnailGeneratorException(String message) {
		super(message);
	}

	public ThumbnailGeneratorException(Throwable cause) {
		super(cause);
	}

	public ThumbnailGeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ThumbnailGeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
