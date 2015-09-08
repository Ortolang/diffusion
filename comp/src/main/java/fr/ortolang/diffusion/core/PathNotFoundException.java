package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class PathNotFoundException extends Exception {

    public PathNotFoundException() {
        super();
    }

    public PathNotFoundException(String message) {
        super(message);
    }

    public PathNotFoundException(Throwable cause) {
        super(cause);
    }

    public PathNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}