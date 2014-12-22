package fr.ortolang.diffusion.api.ssh.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;

import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.api.ssh.session.SSHSession;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

public class DiffusionPasswordAuthenticator implements PasswordAuthenticator {
	
	private static Logger logger = Logger.getLogger(DiffusionPasswordAuthenticator.class.getName());

    @Override
    public boolean authenticate(final String username, final String password, final ServerSession session) {
        try {
        	logger.log(Level.INFO, "performing jaas authentication...");
        	LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(username, password);
            lc.login();
            logger.log(Level.INFO, "try a call to membreship service to validate credentials");
            MembershipService membership = (MembershipService)OrtolangServiceLocator.findService("membership");
            String expected = membership.getProfileKeyForIdentifier(username);
    		String key = membership.getProfileKeyForConnectedIdentifier();
    		lc.logout();
    		logger.log(Level.INFO, "expected: " + expected);
    		logger.log(Level.INFO, "key: " + key);
    		
    		if (key.equals(expected)) {
    			session.setAttribute(new AttributeKey<UsernamePassword>(), new UsernamePassword(username, password));
    			logger.log(Level.INFO, "connected profile [" + key + "] is the expected one [" + expected + "], login OK");
    			if (session instanceof SSHSession) {
    				((SSHSession) session).setLogin(username);
    				((SSHSession) session).setPassword(password);
                } else {
                	logger.log(Level.WARNING, "ServerSession is not of type SSHServerSession : unable to set login/password for futur authentication");
                	return false;
                }
    			return true;
    		} else {
    			logger.log(Level.INFO, "connected profile [" + key + "] is NOT the expected one [" + expected + "], login KO");
    			return false;
    		}
        } catch (Exception e) {
        	logger.log(Level.INFO, "login failed ", e);
            return false;
        }
    }
    
    class UsernamePassword {
    	
    	private String username;
    	private String password;
    	
    	public UsernamePassword(String username, String password) {
    		this.username = username;
    		this.password = password;
    	}
    	
    	public String getUsername() {
    		return username;
    	}
    	
    	public String getPassword() {
    		return password;
    	}
    	
    }
    
}
