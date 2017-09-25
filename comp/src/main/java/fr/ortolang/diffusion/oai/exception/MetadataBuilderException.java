package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class MetadataBuilderException extends Exception {

	public MetadataBuilderException() {
		super();
	}

    public MetadataBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataBuilderException(String message) {
        super(message);
    }

    public MetadataBuilderException(Throwable cause) {
        super(cause);
    }
}
