package fr.ortolang.diffusion.event;

public class EventServiceException extends Exception {

    private static final long serialVersionUID = -1232062160488122720L;

    public EventServiceException() {
    }

    public EventServiceException(String message) {
        super(message);
    }

    public EventServiceException(Throwable cause) {
        super(cause);
    }

    public EventServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
