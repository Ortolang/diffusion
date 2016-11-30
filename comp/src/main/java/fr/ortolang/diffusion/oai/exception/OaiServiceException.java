package fr.ortolang.diffusion.oai.exception;

@SuppressWarnings("serial")
public class OaiServiceException extends Exception {

    public OaiServiceException() {
        super();
    }

    public OaiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OaiServiceException(String message) {
        super(message);
    }

    public OaiServiceException(Throwable cause) {
        super(cause);
    }

}
