package fr.ortolang.diffusion.dump;

public class DumpServiceException extends Exception {

    private static final long serialVersionUID = 6671214972003009648L;

    public DumpServiceException() {
    }

    public DumpServiceException(String message) {
        super(message);
    }

    public DumpServiceException(Throwable cause) {
        super(cause);
    }

    public DumpServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DumpServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
