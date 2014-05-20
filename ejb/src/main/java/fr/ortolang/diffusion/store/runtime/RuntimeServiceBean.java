package fr.ortolang.diffusion.store.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless(name = RuntimeService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class RuntimeServiceBean implements RuntimeService {

	private static Logger logger = Logger.getLogger(RuntimeServiceBean.class.getName());

	
	public RuntimeServiceBean() {
	}
	
	@PostConstruct
	public void init() throws Exception {
		logger.log(Level.INFO, "starting runtime service...");
		logger.log(Level.INFO, "runtime service started");
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.log(Level.INFO, "stopping runtime service...");
		logger.log(Level.INFO, "runtime service stopped");
	}

}
