package fr.ortolang.diffusion.message;

@SuppressWarnings("serial")
public class MessageServiceException extends Exception {

    public MessageServiceException() {
        super();
    }

    public MessageServiceException(String message) {
        super(message);
    }

    public MessageServiceException(Throwable cause) {
        super(cause);
    }

    public MessageServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}