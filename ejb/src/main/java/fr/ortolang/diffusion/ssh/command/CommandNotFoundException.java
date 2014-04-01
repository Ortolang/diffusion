package fr.ortolang.diffusion.ssh.command;

import fr.ortolang.diffusion.ssh.SSHServiceException;


/**
 * Command Not Found Exception
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
@SuppressWarnings(value = "serial")
public class CommandNotFoundException extends SSHServiceException {
    
	public CommandNotFoundException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    public CommandNotFoundException(String message) {
        super(message);
    }

    public CommandNotFoundException(Exception rootCause) {
        super(rootCause);
    }

    public CommandNotFoundException() {
        super();
    }
}
