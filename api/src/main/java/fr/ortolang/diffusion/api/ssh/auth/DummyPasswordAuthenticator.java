package fr.ortolang.diffusion.api.ssh.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;


public class DummyPasswordAuthenticator implements PasswordAuthenticator {
	
	private static Logger logger = Logger.getLogger(DummyPasswordAuthenticator.class.getName());
    
    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
    	logger.log(Level.FINE, "authenticating dummy user: " + username);
        return true;
    }
}
