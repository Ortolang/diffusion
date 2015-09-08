package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class PathAlreadyExistsException extends Exception {

    public PathAlreadyExistsException() {
        super();
    }

    public PathAlreadyExistsException(String message) {
        super(message);
    }

    public PathAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public PathAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}