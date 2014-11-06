package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngine;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.ProcessType;

@Startup
@Local(RuntimeEngine.class)
@Singleton(name = RuntimeEngine.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({ "system", "user" })
@Lock(LockType.READ)
public class ActivitiEngineBean implements RuntimeEngine {

	private Logger logger = Logger.getLogger(ActivitiEngineBean.class.getName());

	@Resource
	private ManagedScheduledExecutorService scheduledExecutor;
	@Resource
	private ManagedExecutorService executor;
	@Resource
	private ContextService contextService;
	@PersistenceUnit(unitName = "ortolangPU")
	private EntityManagerFactory emf;
	
	private ProcessEngine engine;
	
	public ActivitiEngineBean() {
	}

	@PostConstruct
	public void init() {
		logger.log(Level.INFO, "Initilizing EngineServiceBean");
		if (engine == null) {
			ProcessEngineConfiguration config = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
			config.setJpaHandleTransaction(true);
			config.setJpaCloseEntityManager(false);
			config.setJpaEntityManagerFactory(emf);
			config.setMailSessionJndi("java:jboss/mail/Default");
			ActivitiEngineJobExecutor jobExecutor = new ActivitiEngineJobExecutor(scheduledExecutor, executor);
			config.setJobExecutor(jobExecutor);
			config.setJobExecutorActivate(true);
			config.setProcessEngineName("ortolang");
			engine = config.buildProcessEngine();
			engine.getRuntimeService().addEventListener(new ActivitiEngineListener());
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
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deployDefinitions(String[] resources) {
		logger.log(Level.INFO, "Deploying process definitions");
		DeploymentBuilder deployment = engine.getRepositoryService().createDeployment();
		deployment.enableDuplicateFiltering();
		deployment.name("EngineServiceDeployement");
		for (String resource : resources) {
			logger.log(Level.INFO, "Adding process definition resource to deployment : " + resource);
			deployment.addClasspathResource(resource);
		}
		Deployment deploy = deployment.deploy();
		logger.log(Level.INFO, "Process definitions deployed on " + deploy.getDeploymentTime());
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessType> listProcessTypes() throws RuntimeEngineException {
		List<ProcessDefinition> apdefs = engine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
		List<ProcessType> defs = new ArrayList<ProcessType>();
		for (ProcessDefinition apdef : apdefs) {
			defs.add(toProcessType(apdef));
		}
		return defs;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessType getProcessTypeById(String id) throws RuntimeEngineException {
		ProcessDefinition apdef = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(id).singleResult();
		return toProcessType(apdef);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessType getProcessTypeByKey(String key) throws RuntimeEngineException {
		ProcessDefinition apdef = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(key).singleResult();
		return toProcessType(apdef);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcess(String type, String key, Map<String, Object> variables) throws RuntimeEngineException {
		ActivitiProcessRunner runnable = new ActivitiProcessRunner(engine.getRuntimeService(), type, key, variables);
		Runnable ctxRunnable = contextService.createContextualProxy(runnable, Runnable.class);
		scheduledExecutor.schedule(ctxRunnable, 3, TimeUnit.SECONDS);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Process getProcess(String key) throws RuntimeEngineException {
		try {
			ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(key).singleResult();
			return toProcess(instance);
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while getting process instance", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public HumanTask getTask(String id) throws RuntimeEngineException {
		try {
			Task task = engine.getTaskService().createTaskQuery().taskId(id).singleResult();
			return toHumanTask(task);
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while getting task", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listCandidateTasks(String user, List<String> groups) throws RuntimeEngineException {
		try {
			List<HumanTask> ctasks = new ArrayList<HumanTask>();
			
			List<Task> cutasks = engine.getTaskService().createTaskQuery().taskCandidateUser(user).list();
			for (Task task : cutasks) {
				ctasks.add(toHumanTask(task));
			}
			if ( groups != null && groups.size() > 0 ) {
				List<Task> cgtasks = engine.getTaskService().createTaskQuery().taskCandidateGroupIn(groups).list();
				for (Task task : cgtasks) {
					ctasks.add(toHumanTask(task));
				}
			}
			
			return ctasks;
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while listing candidate tasks", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listAssignedTasks(String user) throws RuntimeEngineException {
		try {
			List<HumanTask> atasks = new ArrayList<HumanTask>();
			List<Task> autasks = engine.getTaskService().createTaskQuery().taskAssignee(user).list();
			for (Task task : autasks) {
				atasks.add(toHumanTask(task));
			}
			return atasks;
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while listing assigned tasks", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void claimTask(String id, String assignee) throws RuntimeEngineException {
		try {
			engine.getTaskService().claim(id, assignee);
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while claiming process task", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void completeTask(String id, Map<String, Object> variables) throws RuntimeEngineException {
		ActivitiTaskRunner runnable = new ActivitiTaskRunner(engine.getTaskService(), id, variables);
		Runnable ctxRunnable = contextService.createContextualProxy(runnable, Runnable.class);
		scheduledExecutor.schedule(ctxRunnable, 3, TimeUnit.SECONDS);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void notify(String type) throws RuntimeEngineException {
		
	}
	
	private ProcessType toProcessType(ProcessDefinition def) {
		ProcessType instance = new ProcessType();
		instance.setId(def.getId());
		instance.setName(def.getKey());
		instance.setDescription(def.getDescription());
		instance.setVersion(def.getVersion());
		instance.setSuspended(def.isSuspended());
		return instance;
	}

	private Process toProcess(ProcessInstance pins) {
		Process instance = new Process();
		instance.setId(pins.getBusinessKey());
		instance.setName(pins.getName());
		
		return instance;
	}

	private HumanTask toHumanTask(Task task) {
		HumanTask instance = new HumanTask();
		instance.setId(task.getId());
		instance.setName(task.getName());
		instance.setDescription(task.getDescription());
		instance.setOwner(task.getOwner());
		instance.setAssignee(task.getAssignee());
		instance.setCreationDate(task.getCreateTime());
		instance.setDueDate(task.getDueDate());
		instance.setPriority(task.getPriority());
		return instance;
	}

}