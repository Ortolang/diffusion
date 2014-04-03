package fr.ortolang.diffusion.search;

@SuppressWarnings("serial")
public class SearchServiceException extends Exception {

	public SearchServiceException() {
		super();
	}

	public SearchServiceException(String message) {
		super(message);
	}

	public SearchServiceException(Throwable cause) {
		super(cause);
	}

	public SearchServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}