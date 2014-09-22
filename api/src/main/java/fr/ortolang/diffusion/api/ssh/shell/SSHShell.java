package fr.ortolang.diffusion.api.ssh.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

public class SSHShell implements Command, Runnable, SessionAware {

	private static Logger logger = Logger.getLogger(SSHShell.class.getName());
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment env;
    private ServerSession session;
    private ThreadFactory threadFactory;
    private Thread thread;
    
    public SSHShell(List<String> commands, ThreadFactory threadFactory) {
    	super();
    	this.threadFactory = threadFactory;
    }

    @Override
    public void setInputStream(InputStream in) {
        logger.log(Level.INFO, "setting input stream");
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
    	logger.log(Level.INFO, "setting output stream");
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
    	logger.log(Level.INFO, "setting error stream");
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
    	logger.log(Level.INFO, "setting exit callback");
        this.callback = callback;
    }
    
    @Override
    public void setSession(ServerSession session) {
    	logger.log(Level.INFO, "setting session");
        this.session = session;
    }

    public Environment getEnv() {
        return env;
    }

    @Override
	public void start(Environment env) throws IOException {
    	logger.log(Level.FINE, "Starting shell");
    	this.in = new EchoInputStream(in, out);
		this.env = env;
		thread = threadFactory.newThread(this);
		thread.start();
	}

    @Override
    public void run() {
		try {
			logger.log(Level.INFO, "Starting Interpreter");
			try {
				int cpt = 0;
				int input;
				while ( cpt < 50 && (input = in.read()) != -1 ) {
					cpt++;
					logger.log(Level.FINE, "" + input);
				}
				out.write('\r');
				out.write('\n');
			} catch ( IOException e ) {
				logger.log(Level.SEVERE, "Don't know what happened but problem: ", e);
			}
			logger.log(Level.INFO, "Interpreter stopped, bye");
		} finally {
			try {
				out.flush();
			} catch (IOException err) {
				logger.log(Level.SEVERE, "Error running impl", err);
			}
			try {
				err.flush();
			} catch (IOException err) {
				logger.log(Level.SEVERE, "Error running impl", err);
			}
			callback.onExit(0);
		}
		try {
			session.disconnect(12, "Number of characters reached !!");
		} catch ( IOException e ) {
			logger.log(Level.SEVERE, "Don't know what happened but problem: ", e);
		}
	}

	@Override
	public void destroy() {
		logger.log(Level.FINE, "Shell Destroyed");
		
	}
}
