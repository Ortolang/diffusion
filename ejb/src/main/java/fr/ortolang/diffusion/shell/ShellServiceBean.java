package fr.ortolang.diffusion.shell;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.crsh.standalone.Bootstrap;
import org.jboss.ejb3.annotation.SecurityDomain;

@Startup
@Singleton(name = ShellService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class ShellServiceBean implements ShellService {

	private static Logger logger = Logger.getLogger(ShellServiceBean.class.getName());

	private Bootstrap crash;
	
	public ShellServiceBean() {
		logger.log(Level.FINE, "new shell service instance created");
//		crash = new Bootstrap(this.getClass().getClassLoader());
//		crash.setConfig(OrtolangConfig.getInstance().getProperties());
	}

	@PostConstruct
	public void init() throws Exception {
		logger.log(Level.INFO, "starting shell service...");
//		crash.bootstrap();
		logger.log(Level.INFO, "shell service started");
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.log(Level.INFO, "stopping shell service...");
//		crash.shutdown();
		logger.log(Level.INFO, "shell service stopped");
	}

}