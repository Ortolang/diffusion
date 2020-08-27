package fr.ortolang.diffusion.content;

@SuppressWarnings("serial")
public class ContentSearchServiceException extends Exception {

    public ContentSearchServiceException() {
        super();
    }

    public ContentSearchServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentSearchServiceException(String message) {
        super(message);
    }

    public ContentSearchServiceException(Throwable cause) {
        super(cause);
    }

}
