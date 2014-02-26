package fr.ortolang.diffusion.indexing;

@SuppressWarnings("serial")
public class IndexingServiceException extends Exception {

	public IndexingServiceException() {
		super();
	}

	public IndexingServiceException(String message) {
		super(message);
	}

	public IndexingServiceException(Throwable cause) {
		super(cause);
	}

	public IndexingServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public IndexingServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}