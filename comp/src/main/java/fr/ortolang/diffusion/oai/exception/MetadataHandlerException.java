package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class MetadataHandlerException extends Exception {

	public MetadataHandlerException() {
		super();
	}

    public MetadataHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataHandlerException(String message) {
        super(message);
    }

    public MetadataHandlerException(Throwable cause) {
        super(cause);
    }
}
