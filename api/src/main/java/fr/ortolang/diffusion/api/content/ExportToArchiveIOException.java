package fr.ortolang.diffusion.api.content;

public class ExportToArchiveIOException extends Exception {

    private static final long serialVersionUID = -8487561685007619261L;

    public ExportToArchiveIOException() {
    }

    public ExportToArchiveIOException(String message) {
        super(message);
    }

    public ExportToArchiveIOException(Throwable cause) {
        super(cause);
    }

    public ExportToArchiveIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportToArchiveIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
