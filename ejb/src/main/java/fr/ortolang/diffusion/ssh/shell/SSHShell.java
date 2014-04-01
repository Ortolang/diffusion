package fr.ortolang.diffusion.ssh.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

import bsh.EvalError;
import bsh.Interpreter;


/**
 * A specific SSH Shell instance allowing to connect to the diffusion server using any ssh client.<br/>
 * <br/>
 * Stream should be redirected to a command interpreter or any kind of shell.<br/>
 * <br/>
 * IMPLEMENTATION IS NOT COMPLETE.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 6 october 2009
 */
public class SSHShell implements Command, Runnable, SessionAware {
    private static Logger logger = Logger.getLogger(SSHShell.class.getName());
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment env;
    private ServerSession session;
    private List<String> commands;
    private Interpreter interpreter;
    private Thread thread;
    
    public SSHShell(List<String> commands) {
    	super();
    	this.commands = commands;
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
		
		interpreter = new Interpreter(new InputStreamReader(new EchoInputStream(in, out)), new PrintStream(out), new PrintStream(err), true);
		Field[] fields = interpreter.getClass().getFields();
		for (Field field : fields) {
			logger.log(Level.FINE, field.toString());
		}
		Method[] methods = interpreter.getClass().getDeclaredMethods();
		for (Method method : methods) {
			logger.log(Level.FINE, method.toString());
		}
		logger.log(Level.FINE, interpreter.getClass().getProtectionDomain().getCodeSource().getLocation().toString());
		
		this.env = env;
		for ( String key : env.getEnv().keySet() ) {
			try {
				logger.log(Level.FINE, "Setting interpreter variable : " + key + "=" + env.getEnv().get(key));
				interpreter.set(key, env.getEnv().get(key));
				for (String packageName : commands) {
					interpreter.eval( "importCommands(\"" + packageName + "\");" );
				}
				interpreter.eval( "bsh.system.shutdownOnExit = false" );
				logger.log(Level.FINE, "Setting interpreter prompt");
				interpreter.eval( "bsh.cwd = \"/\"" );
				interpreter.eval( "getBshPrompt() { return \"\\r\\n" + session.getUsername() + "@" + InetAddress.getLocalHost().getHostName() + ":\"; }");
			} catch (EvalError e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		thread = new Thread(this,"Shell-Interpreter");
		thread.start();
		
	}

    @Override
    public void run() {
		try {
			try {
				logger.log(Level.INFO, "Starting Interpreter");

				interpreter.run();
				
				logger.log(Level.INFO, "Interpreter stopped, bye");
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error Executing Interpreter", e);
			}
			logger.log(Level.INFO, "Interpreter completed normally");
		} finally {
			try {
				out.flush();
			} catch (IOException err) {
				logger.log(Level.WARNING, "Error running impl", err);
			}
			try {
				err.flush();
			} catch (IOException err) {
				logger.log(Level.WARNING, "Error running impl", err);
			}
			callback.onExit(0);
		}

	}

	@Override
	public void destroy() {
		logger.log(Level.FINE, "Shell Destroyed");
	}
}
