package fr.ortolang.diffusion.ssh.authenticator;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import fr.ortolang.diffusion.ssh.session.SSHSession;


/**
 * A JAAS Password Authenticator to integrate with existing system authentication.<br/>
 * <br/>
 * A hint is used. First JAAS try is using SourcePlantNamingConvention.SECURITY_DOMAIN server side authentication login config in order to get an error
 * immediately in case of wrong credentials because SourcePlantNamingConvention.CLIENT_SECURITY_DOMAIN stands for first service call. In the SSH client this produce
 * a strange effect that let you authenticate but stand for first command to send you authentication exception.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
public class JaasPasswordAuthenticator implements PasswordAuthenticator {
    private static Logger logger = Logger.getLogger(JaasPasswordAuthenticator.class.getName());

    @Override
    public boolean authenticate(final String username, final String password, final ServerSession session) {
//        try {
//            //Trying to authenticate throw the ServerSide LoginModule stack to check if account is valid.
//            LoginContext lc = new LoginContext("ortolang", getCallbackHandler(username, password.toCharArray()));
//            lc.login();
//            logger.log(Level.FINE, "valid login");
//            lc.logout();
//        } catch (Exception e) {
//            logger.log(Level.FINE, "invalid login and/or password");
//            return false;
//        }

        try {
//            Now that login/password are valid, performing a client login to ensure Security Association process.
            LoginContext lc = new LoginContext("ortolang", getCallbackHandler(username, password.toCharArray()));
            lc.login();

            if (session instanceof SSHSession) {
                ((SSHSession) session).setLoginContext(lc);
            } else {
                logger.log(Level.WARNING, "ServerSession is not of type SSHServerSession : unable to set LoginContext for futur logout");
            }
            logger.log(Level.FINE, "user logged");
            return true;
        } catch (Exception e) {
            logger.log(Level.FINE, "login failed");

            return false;
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private CallbackHandler getCallbackHandler(String username, char[] password) throws Exception {
    	logger.log(Level.FINE, "Trying to find a suitable CallbackHandler for the server");
    	Class handlerClass = null;
    	CallbackHandler handler = null;
    	try {
    		logger.log(Level.FINE, "Trying to load CallbackHandler class for JBoss server : UsernamePasswordHandler");
	    	handlerClass = Class.forName("org.jboss.security.auth.callback.UsernamePasswordHandler");
	    } catch ( ClassNotFoundException cnfe ) {
	    	logger.log(Level.FINE, "Unable to load JBoss CallbackHandler");
	    }
    	if ( handlerClass != null ) {
	    	try {
	    		logger.log(Level.FINE, "Trying to instanciate the CallbackHandler");
	    		handler = (CallbackHandler) handlerClass.getDeclaredConstructor(username.getClass(), password.getClass()).newInstance(username, password);
	    		return handler;
	    	} catch ( Exception e ) {
				logger.log(Level.FINE, "Unable to instanciate handler");
	    		throw new Exception("unable to instanciate the CallbackHandler", e);
	    	}
    	} else {
    		throw new Exception("unable to find a known CallbackHandler in the Classloader");
    	}
    }    
}