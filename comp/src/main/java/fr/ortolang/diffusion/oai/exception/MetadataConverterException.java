package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class MetadataConverterException extends Exception {

	public MetadataConverterException() {
		super();
	}

    public MetadataConverterException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataConverterException(String message) {
        super(message);
    }

    public MetadataConverterException(Throwable cause) {
        super(cause);
    }
}
