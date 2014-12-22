package fr.ortolang.diffusion.api.ssh.auth;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;

import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.common.util.Base64;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.api.ssh.session.SSHSession;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

public class DiffusionPublicKeyAuthenticator implements PublickeyAuthenticator {
	
	private static Logger logger = Logger.getLogger(DiffusionPublicKeyAuthenticator.class.getName());

	@Override
	public boolean authenticate(String username, PublicKey pubkey, ServerSession session) {
		try {
        	logger.log(Level.INFO, "performing jaas authentication...");
        	logger.log(Level.FINE, "public-key: " + getString(pubkey));
        	
        	LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(username, getString(pubkey));
            lc.login();
            logger.log(Level.FINE, "try a call to membreship service to validate credentials");
            MembershipService membership = (MembershipService)OrtolangServiceLocator.findService("membership");
            String expected = membership.getProfileKeyForIdentifier(username);
    		String key = membership.getProfileKeyForConnectedIdentifier();
    		lc.logout();
    		logger.log(Level.FINE, "expected: " + expected);
    		logger.log(Level.FINE, "key: " + key);
    		
    		if (key.equals(expected)) {
    			session.setAttribute(new AttributeKey<UsernamePassword>(), new UsernamePassword(username, getString(pubkey)));
    			logger.log(Level.INFO, "connected profile [" + key + "] is the expected one [" + expected + "], login OK");
    			if (session instanceof SSHSession) {
    				((SSHSession) session).setLogin(username);
    				((SSHSession) session).setPassword(getString(pubkey));
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
	
	private String getString(PublicKey key) throws FailedLoginException {
        try {
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsa = (DSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                write(dos, "ssh-dss");
                write(dos, dsa.getParams().getP());
                write(dos, dsa.getParams().getQ());
                write(dos, dsa.getParams().getG());
                write(dos, dsa.getY());
                dos.close();
                return new String(Base64.encodeBase64(baos.toByteArray()));
            } else if (key instanceof RSAKey) {
                RSAPublicKey rsa = (RSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                write(dos, "ssh-rsa");
                write(dos, rsa.getPublicExponent());
                write(dos, rsa.getModulus());
                dos.close();
                return new String(Base64.encodeBase64(baos.toByteArray()));
            } else {
                throw new FailedLoginException("Unsupported key type " + key.getClass().toString());
            }
        } catch (IOException e) {
            throw new FailedLoginException("Unable to check public key");
        }
    }

    private void write(DataOutputStream dos, BigInteger integer) throws IOException {
        byte[] data = integer.toByteArray();
        dos.writeInt(data.length);
        dos.write(data, 0, data.length);
    }

    private void write(DataOutputStream dos, String str) throws IOException {
        byte[] data = str.getBytes();
        dos.writeInt(data.length);
        dos.write(data);
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
