package fr.ortolang.diffusion.ssh.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;


/**
 * The classic NoOP Command, returned when command is not found.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
public class NoOPCommand implements Command {
    private static Logger logger = Logger.getLogger(NoOPCommand.class.getName());
    private ExitCallback callback;
    private OutputStream err;

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setInputStream(InputStream in) {
    }

    public void setOutputStream(OutputStream out) {
    }

    @Override
    public void start(Environment env) throws IOException {
        logger.log(Level.INFO,"Executing NoOP Command");
        err.write("Command not found\r\n".getBytes());
        err.flush();
        callback.onExit(-1);
    }

	@Override
	public void destroy() {
	}

}
