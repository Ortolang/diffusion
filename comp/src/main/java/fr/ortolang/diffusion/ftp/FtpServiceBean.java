package fr.ortolang.diffusion.ftp;

import java.util.Collections;
import java.util.Map;
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
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.ftp.filesystem.OrtolangFileSystemFactory;
import fr.ortolang.diffusion.ftp.user.OrtolangUserManager;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Startup
@Singleton(name = FtpServiceBean.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("system")
public class FtpServiceBean implements FtpService {
	
	private static final Logger LOGGER = Logger.getLogger(FtpServiceBean.class.getName());
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    @EJB
    private MembershipService membership;
    @EJB
    private RegistryService registry;
    
    private OrtolangFileSystemFactory fsFactory;
	private OrtolangUserManager userManager;
	private ConnectionConfigFactory cFactory;
	private FtpServer server;

	public FtpServiceBean() {
	    LOGGER.log(Level.FINE, "Instanciating ftp service");
    	FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();
		factory.setPort(Integer.parseInt(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_PORT)));
		serverFactory.addListener("default", factory.createListener());
		fsFactory = new OrtolangFileSystemFactory();
		serverFactory.setFileSystem(fsFactory);
		cFactory = new ConnectionConfigFactory();
		serverFactory.setConnectionConfig(cFactory.createConnectionConfig());
		userManager = new OrtolangUserManager();
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
            return membership.systemReadProfileSecret(username);
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
        //TODO provide infos about active connections, config, ports, etc...
        return Collections.emptyMap();
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
    public OrtolangObject findObject(String key) throws OrtolangException, AccessDeniedException, KeyNotFoundException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
        throw new OrtolangException("this service does not managed any object");
    }

}
