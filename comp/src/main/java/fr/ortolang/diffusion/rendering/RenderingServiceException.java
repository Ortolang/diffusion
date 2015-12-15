package fr.ortolang.diffusion.rendering;

public class RenderingServiceException extends Exception {

    private static final long serialVersionUID = -932630140236832319L;

    public RenderingServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RenderingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RenderingServiceException(String message) {
        super(message);
    }

    public RenderingServiceException(Throwable cause) {
        super(cause);
    }

}
