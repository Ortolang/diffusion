package fr.ortolang.diffusion.ssh.authenticator;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;


/**
 * A DummyPassword Authenticator to bypass JAAS authentication mechanism and allow testing or ease development.<br/>
 * <br/>
 * NOT TO USE IN PRODUCTION
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
public class DummyPasswordAuthenticator implements PasswordAuthenticator {
	
	private static Logger logger = Logger.getLogger(DummyPasswordAuthenticator.class.getName());
    
    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
    	logger.log(Level.INFO, "authentication always ok");
        return true;
    }
}
