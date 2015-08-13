package fr.ortolang.diffusion.ftp;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.ftp.filesystem.OrtolangFileSystemFactory;
import fr.ortolang.diffusion.ftp.user.OrtolangUserManager;

@Startup
@Singleton(name = FtpServiceBean.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class FtpServiceBean implements FtpService, FtpServiceAdmin {
	
	private static final Logger LOGGER = Logger.getLogger(FtpServiceBean.class.getName());
	
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
	public Map<String, String> getServiceInfos() throws FtpServiceException     {
	    Map<String, String>infos = new HashMap<String, String> ();
        return infos;
	}

}
