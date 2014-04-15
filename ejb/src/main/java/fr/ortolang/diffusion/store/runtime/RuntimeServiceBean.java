package fr.ortolang.diffusion.store.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.ejb3.annotation.SecurityDomain;

@Startup
@Singleton(name = RuntimeService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class RuntimeServiceBean implements RuntimeService {

	private static Logger logger = Logger.getLogger(RuntimeServiceBean.class.getName());

	@PersistenceUnit(unitName="ortolangPU")
	private EntityManagerFactory emf;
	
//	private RuntimeEnvironment env;
//	private RuntimeManager manager;
//	private RuntimeEngine runtime;
	
	public RuntimeServiceBean() {
		logger.log(Level.FINE, "new runtime service instance created");
	}
	
	//KieSession ksession = runtimeEngine.getKieSession();

	@PostConstruct
	public void init() throws Exception {
		logger.log(Level.INFO, "starting runtime service...");
//		RuntimeEnvironmentBuilderFactory factory = RuntimeEnvironmentBuilder.Factory.get();
//		logger.log(Level.FINE, "runtime factory retreived");
//		RuntimeEnvironmentBuilder builder = factory.newDefaultBuilder();
//		logger.log(Level.FINE, "environment builder created");
//		env = builder.entityManagerFactory(emf).get();
//		logger.log(Level.FINE, "environment retreived");
//				//addAsset(ResourceFactory.newClassPathResource("BPMN2-ScriptTask.bpmn2"), ResourceType.BPMN2)
//		manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(env);
//		runtime = manager.getRuntimeEngine(EmptyContext.get());
		logger.log(Level.INFO, "runtime service started");
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.log(Level.INFO, "stopping runtime service...");
//		manager.disposeRuntimeEngine(runtime);
//		manager.close();
		logger.log(Level.INFO, "runtime service stopped");
	}

}
