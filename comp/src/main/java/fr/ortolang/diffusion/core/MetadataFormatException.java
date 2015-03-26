package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class MetadataFormatException extends Exception {

	public MetadataFormatException() {
		super();
	}

	public MetadataFormatException(String message) {
		super(message);
	}

	public MetadataFormatException(Throwable cause) {
		super(cause);
	}

	public MetadataFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}