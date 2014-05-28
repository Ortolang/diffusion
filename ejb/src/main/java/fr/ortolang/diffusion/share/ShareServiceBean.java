package fr.ortolang.diffusion.share;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.ejb3.annotation.SecurityDomain;

@Startup
@Singleton(name = ShareService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class ShareServiceBean implements ShareService {

	private static Logger logger = Logger.getLogger(ShareServiceBean.class.getName());
	
	public ShareServiceBean() {
		logger.log(Level.FINE, "new share service instance created");
	}

	@PostConstruct
	public void init() throws Exception {
		logger.log(Level.INFO, "starting share service...");

		logger.log(Level.INFO, "share service started");
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.log(Level.INFO, "stopping share service...");

		logger.log(Level.INFO, "share service stopped");
	}

}
