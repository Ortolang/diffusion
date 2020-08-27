package fr.ortolang.diffusion.content;

@SuppressWarnings("serial")
public class ContentSearchNotFoundException extends Exception {

    public ContentSearchNotFoundException() {
        super();
    }

    public ContentSearchNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentSearchNotFoundException(String message) {
        super(message);
    }

    public ContentSearchNotFoundException(Throwable cause) {
        super(cause);
    }

}
