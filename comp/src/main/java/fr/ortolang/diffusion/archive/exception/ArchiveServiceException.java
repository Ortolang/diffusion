package fr.ortolang.diffusion.archive.exception;

@SuppressWarnings("serial")
public class ArchiveServiceException extends Exception {
    public ArchiveServiceException() {
        super();
    }
    public ArchiveServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    public ArchiveServiceException(String message) {
        super(message);
    }
}