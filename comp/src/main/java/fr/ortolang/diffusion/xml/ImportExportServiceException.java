package fr.ortolang.diffusion.xml;

public class ImportExportServiceException extends Exception {

    private static final long serialVersionUID = 6671214972003009648L;

    public ImportExportServiceException() {
    }

    public ImportExportServiceException(String message) {
        super(message);
    }

    public ImportExportServiceException(Throwable cause) {
        super(cause);
    }

    public ImportExportServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImportExportServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
