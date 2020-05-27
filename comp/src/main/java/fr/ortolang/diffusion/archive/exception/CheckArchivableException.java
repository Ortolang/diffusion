package fr.ortolang.diffusion.archive.exception;

@SuppressWarnings("serial")
public class CheckArchivableException extends Exception {
    public CheckArchivableException() {
        super();
    }
    public CheckArchivableException(String message, Throwable cause) {
        super(message, cause);
    }
    public CheckArchivableException(String message) {
        super(message);
    }
}