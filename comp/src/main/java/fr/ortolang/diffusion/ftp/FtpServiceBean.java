package fr.ortolang.diffusion.ftp;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.jboss.ejb3.annotation.SecurityDomain;

@Local(FtpServiceBean.class)
@Startup
@Singleton(name = FtpServiceBean.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class FtpServiceBean implements FtpService {
	
	private static final Logger LOGGER = Logger.getLogger(FtpServiceBean.class.getName());
	
	private FtpServer server;

	public FtpServiceBean() {
		FtpServerFactory serverFactory = new FtpServerFactory();
		
		
		server = serverFactory.createServer();
	}
	
	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service, starting server");
//		try {
//			server.start();
//		} catch (FtpException e) {
//			LOGGER.log(Level.SEVERE, "unable to start ftp server", e);
//		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.log(Level.INFO, "Shutting down service, stopping server");
		//server.stop();
	}

	@Override
	public void suspend() {
		server.suspend();
	}

	@Override
	public void resume() {
		server.resume();
	}

}
