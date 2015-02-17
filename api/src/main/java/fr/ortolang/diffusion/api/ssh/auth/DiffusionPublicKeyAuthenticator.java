package fr.ortolang.diffusion.api.ssh.auth;

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
