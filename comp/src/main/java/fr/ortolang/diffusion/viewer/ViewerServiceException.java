package fr.ortolang.diffusion.viewer;

public class ViewerServiceException extends Exception {

    private static final long serialVersionUID = -932630140236832319L;

    public ViewerServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ViewerServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ViewerServiceException(String message) {
        super(message);
    }

    public ViewerServiceException(Throwable cause) {
        super(cause);
    }

}
