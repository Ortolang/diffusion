package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class MetadataPrefixUnknownException extends Exception {

	public MetadataPrefixUnknownException() {
		super();
	}

	public MetadataPrefixUnknownException(String message) {
		super(message);
	}

	public MetadataPrefixUnknownException(Throwable cause) {
		super(cause);
	}

	public MetadataPrefixUnknownException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetadataPrefixUnknownException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
