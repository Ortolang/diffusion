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
	
	private static final Logger LOGGER = Logger.getLogger(DiffusionPasswordAuthenticator.class.getName());

    @Override
    public boolean authenticate(final String username, final String password, final ServerSession session) {
        try {
        	LOGGER.log(Level.INFO, "performing jaas authentication...");
        	LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(username, password);
            lc.login();
            LOGGER.log(Level.FINE, "try a call to membreship service to validate credentials");
            MembershipService membership = (MembershipService)OrtolangServiceLocator.findService("membership");
            String expected = membership.getProfileKeyForIdentifier(username);
    		String key = membership.getProfileKeyForConnectedIdentifier();
    		lc.logout();
    		LOGGER.log(Level.FINEST, "expected: " + expected);
    		LOGGER.log(Level.FINEST, "key: " + key);
    		
    		if (key.equals(expected)) {
    			session.setAttribute(new AttributeKey<UsernamePassword>(), new UsernamePassword(username, password));
    			LOGGER.log(Level.FINE, "connected profile [" + key + "] is the expected one [" + expected + "], login OK");
    			if (session instanceof SSHSession) {
    				((SSHSession) session).setLogin(username);
    				((SSHSession) session).setPassword(password);
                } else {
                	LOGGER.log(Level.WARNING, "ServerSession is not of type SSHServerSession : unable to set login/password for futur authentication");
                	return false;
                }
    			return true;
    		} else {
    			LOGGER.log(Level.FINE, "connected profile [" + key + "] is NOT the expected one [" + expected + "], login KO");
    			return false;
    		}
        } catch (Exception e) {
        	LOGGER.log(Level.WARNING, "login failed ", e);
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
