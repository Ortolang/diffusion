package fr.ortolang.diffusion.workflow.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.jboss.ejb3.annotation.SecurityDomain;

@Startup
@Local(ProcessEngineService.class)
@Singleton(name = ProcessEngineService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({"system", "user"})
@Lock(LockType.READ)
public class ProcessEngineServiceBean implements ProcessEngineService {
	
	private Logger logger = Logger.getLogger(ProcessEngineServiceBean.class.getName());
	
	@Resource
	private ManagedExecutorService executor;
	@PersistenceUnit(unitName = "ortolangPU")
	private EntityManagerFactory emf;
	private ProcessEngine engine;
	
	public ProcessEngineServiceBean() {
	}
	
	@PostConstruct
	public void init() {
		logger.log(Level.INFO, "Initilizing EngineServiceBean");
		if (engine == null) {
			ProcessEngineConfiguration config = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
			config.setJpaHandleTransaction(false);
			config.setJpaCloseEntityManager(false);
			config.setJpaEntityManagerFactory(emf);
			config.setDatabaseSchemaUpdate("true");
			config.setMailSessionJndi("java:jboss/mail/Default");
			ProcessEngineJobExecutor jobExecutor = new ProcessEngineJobExecutor(executor);
			config.setJobExecutor(jobExecutor);
			config.setJobExecutorActivate(true);
			engine = config.buildProcessEngine();
			logger.log(Level.INFO, "Activiti Engine created: " + engine.getName());
		}
		logger.log(Level.INFO, "EngineServiceBean initialized");
	}

	@PreDestroy
	public void dispose() {
		logger.log(Level.INFO, "Stopping EngineServiceBean");
		if (engine != null) {
			engine.close();
		}
		logger.log(Level.INFO, "EngineServiceBean stopped");
	}
	
	@Override
	public RepositoryService getRepositoryService() throws ProcessEngineServiceException {
		return engine.getRepositoryService();
	}
	
	@Override
	public TaskService getTaskService() throws ProcessEngineServiceException {
		return engine.getTaskService();
	}
	
	@Override
	public RuntimeService getRuntimeService() throws ProcessEngineServiceException {
		return engine.getRuntimeService();
	}
	
}
