package fr.ortolang.diffusion.system;

@SuppressWarnings("serial")
public class SystemServiceException extends Exception {

    public SystemServiceException() {
    }

    public SystemServiceException(String message) {
        super(message);
    }

    public SystemServiceException(Throwable cause) {
        super(cause);
    }

    public SystemServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
