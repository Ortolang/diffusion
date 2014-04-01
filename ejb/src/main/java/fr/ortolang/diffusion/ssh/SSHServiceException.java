package fr.ortolang.diffusion.ssh;

/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
@SuppressWarnings("serial")
public class SSHServiceException extends Exception {
    
	public SSHServiceException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    public SSHServiceException(String message) {
        super(message);
    }

    public SSHServiceException(Exception rootCause) {
        super(rootCause);
    }

    public SSHServiceException() {
        super();
    }
}
