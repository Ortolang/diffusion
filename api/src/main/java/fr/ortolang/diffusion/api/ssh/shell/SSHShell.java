package fr.ortolang.diffusion.api.ssh.shell;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

	private static final Logger lOGGER = Logger.getLogger(SSHShell.class.getName());
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
        lOGGER.log(Level.FINE, "setting input stream");
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
    	lOGGER.log(Level.FINE, "setting output stream");
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
    	lOGGER.log(Level.FINE, "setting error stream");
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
    	lOGGER.log(Level.FINE, "setting exit callback");
        this.callback = callback;
    }
    
    @Override
    public void setSession(ServerSession session) {
    	lOGGER.log(Level.FINE, "setting session");
        this.session = session;
    }

    public Environment getEnv() {
        return env;
    }

    @Override
	public void start(Environment env) throws IOException {
    	lOGGER.log(Level.FINE, "Starting shell");
    	this.in = new EchoInputStream(in, out);
		this.env = env;
		thread = threadFactory.newThread(this);
		thread.start();
	}

    @Override
    public void run() {
		try {
			lOGGER.log(Level.INFO, "Starting Interpreter");
			try {
				int cpt = 0;
				int input;
				while ( cpt < 50 && (input = in.read()) != -1 ) {
					cpt++;
					lOGGER.log(Level.FINE, "" + input);
				}
				out.write('\r');
				out.write('\n');
			} catch ( IOException e ) {
				lOGGER.log(Level.SEVERE, "Don't know what happened but problem: ", e);
			}
			lOGGER.log(Level.INFO, "Interpreter stopped, bye");
		} finally {
			try {
				out.flush();
			} catch (IOException err) {
				lOGGER.log(Level.SEVERE, "Error running impl", err);
			}
			try {
				err.flush();
			} catch (IOException err) {
				lOGGER.log(Level.SEVERE, "Error running impl", err);
			}
			callback.onExit(0);
		}
		try {
			session.disconnect(12, "Number of characters reached !!");
		} catch ( IOException e ) {
			lOGGER.log(Level.SEVERE, "Don't know what happened but problem: ", e);
		}
	}

	@Override
	public void destroy() {
		lOGGER.log(Level.FINE, "Shell Destroyed");
		
	}
}
