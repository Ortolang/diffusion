package fr.ortolang.diffusion.ftp;

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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.ftp.filesystem.OrtolangFileSystemFactory;
import fr.ortolang.diffusion.ftp.user.OrtolangUserManager;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

@Startup
@Singleton(name = FtpServiceBean.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("admin")
public class FtpServiceBean implements FtpService {
	
	private static final Logger LOGGER = Logger.getLogger(FtpServiceBean.class.getName());
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };

    @EJB
    private MembershipService membership;
    @EJB
    private RegistryService registry;

    private ListenerFactory lFactory;
    private Listener listener;
    private ConnectionConfigFactory cFactory;
	private FtpServer server;

	public FtpServiceBean() {
	    LOGGER.log(Level.FINE, "Instantiating ftp service");
    	FtpServerFactory serverFactory = new FtpServerFactory();
    	lFactory = new ListenerFactory();
    	lFactory.setPort(Integer.parseInt(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_PORT)));
        DataConnectionConfigurationFactory dcFactory = new DataConnectionConfigurationFactory();
        dcFactory.setPassivePorts(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_PASSIVE_PORTS));
        lFactory.setDataConnectionConfiguration(dcFactory.createDataConnectionConfiguration());
        boolean sslActive = Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_SSL));
        if ( sslActive ) {
            SslConfigurationFactory sslcFactory = new SslConfigurationFactory();
        	sslcFactory.setKeystoreFile(new File(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SSL_KEYSTORE_FILE)));
        	sslcFactory.setKeystorePassword(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SSL_KEYSTORE_PASSWORD));
        	sslcFactory.setKeyAlias(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SSL_KEY_PASSWORD));
        	sslcFactory.setKeyPassword(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.SSL_KEY_ALIAS));
        	dcFactory.setSslConfiguration(sslcFactory.createSslConfiguration());
            dcFactory.setImplicitSsl(true);
            lFactory.setSslConfiguration(sslcFactory.createSslConfiguration());
            lFactory.setImplicitSsl(true);
        }
        listener = lFactory.createListener();
    	serverFactory.addListener("default", listener);
        OrtolangFileSystemFactory fsFactory = new OrtolangFileSystemFactory();
		serverFactory.setFileSystem(fsFactory);
		cFactory = new ConnectionConfigFactory();
		serverFactory.setConnectionConfig(cFactory.createConnectionConfig());
        OrtolangUserManager userManager = new OrtolangUserManager();
		serverFactory.setUserManager(userManager);
		server = serverFactory.createServer();
	}
	
	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service, starting server");
		try {
			server.start();
		} catch (FtpException e) {
			LOGGER.log(Level.SEVERE, "unable to start ftp server", e);
		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.log(Level.INFO, "Shutting down service, stopping server");
		server.stop();
	}

	@Override
	public void suspend() {
		server.suspend();
	}

	@Override
	public void resume() {
		server.resume();
	}
	
	@Override
    public Set<FtpSession> getActiveSessions() throws FtpServiceException {
        Set<FtpSession> sessions = new HashSet<FtpSession> ();
        for ( FtpIoSession ios : listener.getActiveSessions() ) {
            sessions.add(FtpSession.fromFtpIoSession(ios));
        }
        return sessions;
    }
	
	@Override
    public boolean checkUserExistence(String username) throws FtpServiceException {
	    OrtolangObjectIdentifier uid = new OrtolangObjectIdentifier(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, username);
	    try {
	        registry.lookup(uid);
	        return true;
	    } catch ( IdentifierNotRegisteredException e ) {
	        return false;
	    } catch (RegistryServiceException e ) {
	        throw new FtpServiceException ("unable to check if user exists", e);
	    }
    }

    @Override
    public boolean checkUserAuthentication(String username, String password) throws FtpServiceException {
        try {
            return membership.systemValidateTOTP(username, password);
        } catch (MembershipServiceException | KeyNotFoundException e) {
            throw new FtpServiceException("unable to check user authentication", e);
        }
    }

    @Override
    public String getInternalAuthenticationPassword(String username) throws FtpServiceException {
        try {
            return membership.systemReadProfile(username).getSecret();
        } catch (MembershipServiceException | KeyNotFoundException e) {
            throw new FtpServiceException("unable to get internal user password", e);
        }
    }
	
	//Service methods
	
	@Override
    public String getServiceName() {
        return FtpService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String>infos = new HashMap<String, String> ();
        infos.put(INFO_SERVER_HOST, lFactory.getServerAddress());
        infos.put(INFO_SERVER_PORT, Integer.toString(lFactory.getPort()));
        if (server.isStopped()) {
            infos.put(INFO_SERVER_STATE, "stopped");
        } else if (server.isSuspended()) {
            infos.put(INFO_SERVER_STATE, "suspended");
        } else {
            infos.put(INFO_SERVER_STATE, "started");
        }
        infos.put(INFO_LOGIN_FAILURE_DELAY, Integer.toString(cFactory.getLoginFailureDelay()));
        infos.put(INFO_ANON_LOGIN_ENABLES, Boolean.toString(cFactory.isAnonymousLoginEnabled()));
        infos.put(INFO_MAX_ANON_LOGIN, Integer.toString(cFactory.getMaxAnonymousLogins()));
        infos.put(INFO_MAX_LOGIN, Integer.toString(cFactory.getMaxLogins()));
        infos.put(INFO_MAX_LOGIN_FAILURES, Integer.toString(cFactory.getMaxLoginFailures()));
        infos.put(INFO_MAX_THREADS, Integer.toString(cFactory.getMaxThreads()));
        infos.put(INFO_ACTIVE_SESSIONS, Integer.toString(listener.getActiveSessions().size()));
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
    
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

}
