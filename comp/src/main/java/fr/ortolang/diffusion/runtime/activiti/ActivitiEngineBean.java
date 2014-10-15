package fr.ortolang.diffusion.runtime.activiti;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.runtime.RuntimeEngine;
import fr.ortolang.diffusion.runtime.RuntimeEngineException;
import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.entity.ProcessTask;

@Startup
@Local(RuntimeEngine.class)
@Singleton(name = RuntimeEngine.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({"system", "user"})
@Lock(LockType.READ)
public class ActivitiEngineBean implements RuntimeEngine {
	
	private Logger logger = Logger.getLogger(ActivitiEngineBean.class.getName());
	
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
			config.setJpaHandleTransaction(false);
			config.setJpaCloseEntityManager(false);
			config.setJpaEntityManagerFactory(emf);
			config.setDatabaseSchemaUpdate("create-drop");
			config.setMailSessionJndi("java:jboss/mail/Default");
			ActivitiEngineJobExecutor jobExecutor = new ActivitiEngineJobExecutor(executor);
			config.setJobExecutor(jobExecutor);
			config.setJobExecutorActivate(true);
			engine = config.buildProcessEngine();
			engine.getRuntimeService().addEventListener(new ActivitiEngineEventListener());
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
	public String deployProcessDefinition(String name, InputStream definition) throws RuntimeEngineException {
		if ( !name.endsWith(".bpmn") ) {
			name += ".bpmn";
		}
		try {
			Deployment deploy = engine.getRepositoryService().createDeployment().addInputStream(name, definition).enableDuplicateFiltering().deploy();
			String pdefid = engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult().getId();
			return pdefid;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while deploying process definition", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessDefinition> listProcessDefinitions() throws RuntimeEngineException {
		List<org.activiti.engine.repository.ProcessDefinition> apdefs = engine.getRepositoryService().createProcessDefinitionQuery().list();
		List<ProcessDefinition> defs = new ArrayList<ProcessDefinition> ();
		for (org.activiti.engine.repository.ProcessDefinition apdef : apdefs) {
			defs.add(toProcessDefinition(apdef));
		}
		return defs;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessDefinition getProcessDefinition(String id) throws RuntimeEngineException {
		org.activiti.engine.repository.ProcessDefinition apdef = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(id).singleResult();
		return toProcessDefinition(apdef);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] getProcessDefinitionModel(String id) throws RuntimeEngineException {
		try {
			InputStream model = engine.getRepositoryService().getProcessModel(id);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				IOUtils.copy(model, baos);
			} finally {
				IOUtils.closeQuietly(model);
			}
			return baos.toByteArray();
		} catch ( ActivitiObjectNotFoundException | IOException e ) {
			throw new RuntimeEngineException("unexpected error while getting  process definition model", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] getProcessDefinitionDiagram(String id) throws RuntimeEngineException {
		try {
			InputStream model = engine.getRepositoryService().getProcessDiagram(id);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				IOUtils.copy(model, baos);
			} finally {
				IOUtils.closeQuietly(model);
			}
			return baos.toByteArray();
		} catch ( ActivitiException | IOException e ) {
			throw new RuntimeEngineException("unexpected error while getting process definition diagram", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void suspendProcessDefinition(String id) throws RuntimeEngineException {
		try {
			engine.getRepositoryService().suspendProcessDefinitionById(id);
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while suspending process definition", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessDefinition findLatestProcessDefinitionsForName(String name) throws RuntimeEngineException {
		try {
			org.activiti.engine.repository.ProcessDefinition def = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(name).latestVersion().singleResult();
			return toProcessDefinition(def);
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while searching latest process definition by name", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcessInstance(String processDefinitionId, String businessKey, Map<String, Object> variables) throws RuntimeEngineException {
		ActivitiProcessRunner runner = new ActivitiProcessRunner(engine.getRuntimeService(), processDefinitionId, businessKey, variables);
		Runnable ctxRunner = contextService.createContextualProxy(runner, Runnable.class);
		executor.execute(ctxRunner);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessInstance getProcessInstance(String businessKey) throws RuntimeEngineException {
		try {
			org.activiti.engine.runtime.ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
			return toProcessInstance(instance);
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while getting process instance", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessInstance> listProcessInstances(String initier, boolean active) throws RuntimeEngineException {
		try {
			ProcessInstanceQuery query = engine.getRuntimeService().createProcessInstanceQuery().includeProcessVariables();
			if ( initier != null && initier.length() > 0 ) {
				query.variableValueEquals(ProcessInstance.INITIER, initier);
			} 
			if ( active ) {
				query.active();
			}
			List<org.activiti.engine.runtime.ProcessInstance> pinstances = query.list();
			List<ProcessInstance> instances = new ArrayList<ProcessInstance> ();
			for (org.activiti.engine.runtime.ProcessInstance pinstance : pinstances) {
				instances.add(toProcessInstance(pinstance));
			}
			return instances;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while listing process instances", e);
		}
	}
	
	public List<ProcessTask> listAllProcessTasks() throws RuntimeEngineException {
		try {
			List<ProcessTask> tasks = new ArrayList<ProcessTask> ();
			List<Task> ptasks = engine.getTaskService().createTaskQuery().list();
			for (Task task : ptasks) {
				tasks.add(toProcessTask(task));
			}
			return tasks;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while listing all process task", e);
		}
	}
	
	public List<ProcessTask> listCandidateGroupsProcessTasks(List<String> candidateGroups) throws RuntimeEngineException {
		try {
			List<ProcessTask> tasks = new ArrayList<ProcessTask> ();
			List<Task> ptasks = engine.getTaskService().createTaskQuery().taskCandidateGroupIn(candidateGroups).list();
			for (Task task : ptasks) {
				tasks.add(toProcessTask(task));
			}
			return tasks;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while listing candidate groups process task", e);
		}
	}
	
	public List<ProcessTask> listCandidateProcessTasks(String candidateUser) throws RuntimeEngineException {
		try {
			List<ProcessTask> tasks = new ArrayList<ProcessTask> ();
			List<Task> ptasks = engine.getTaskService().createTaskQuery().taskCandidateUser(candidateUser).list();
			for (Task task : ptasks) {
				tasks.add(toProcessTask(task));
			}
			return tasks;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while listing candidate user process task", e);
		}
	}
	
	public List<ProcessTask> listAssignedProcessTasks(String assigneeUser) throws RuntimeEngineException {
		try {
			List<ProcessTask> tasks = new ArrayList<ProcessTask> ();
			List<Task> ptasks = engine.getTaskService().createTaskQuery().taskAssignee(assigneeUser).list();
			for (Task task : ptasks) {
				tasks.add(toProcessTask(task));
			}
			return tasks;
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while listing assigned user process task", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void claimProcessTask(String id, String assigneeUser) throws RuntimeEngineException {
		try {
			engine.getTaskService().claim(id, assigneeUser);
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while claiming process task", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void completeProcessTask(String id, Map<String, Object> variables) throws RuntimeEngineException {
		try {
			engine.getTaskService().complete(id, variables);
		} catch ( ActivitiException e ) {
			throw new RuntimeEngineException("unexpected error while completing process task", e);
		}
	}

	private ProcessDefinition toProcessDefinition(org.activiti.engine.repository.ProcessDefinition def) {
		ProcessDefinition instance = new ProcessDefinition();
		instance.setId(def.getId());
		instance.setName(def.getKey());
		instance.setFriendlyName(def.getName());
		instance.setDescription(def.getDescription());
		instance.setVersion(def.getVersion());
		instance.setSuspended(def.isSuspended());
		return instance;
	}
	
	private ProcessInstance toProcessInstance(org.activiti.engine.runtime.ProcessInstance pins) {
		ProcessInstance instance = new ProcessInstance();
		instance.setId(pins.getBusinessKey());
		instance.setName(pins.getName());
		instance.setParams(pins.getProcessVariables());
		instance.setDefinitionId(pins.getProcessDefinitionId());
		instance.setActivityId(pins.getActivityId());
		instance.setInitier((String) pins.getProcessVariables().get(ProcessInstance.INITIER));
		instance.setSuspended(pins.isSuspended());
		return instance;
	}
	
	private ProcessTask toProcessTask(Task task) {
		ProcessTask instance = new ProcessTask();
		instance.setId(task.getId());
		instance.setName(task.getName());
		instance.setDescription(task.getDescription());
		instance.setOwner(task.getOwner());
		instance.setAssignee(task.getAssignee());
		instance.setCategory(task.getCategory());
		instance.setCreationDate(task.getCreateTime());
		instance.setDueDate(task.getDueDate());
		instance.setExecutionId(task.getExecutionId());
		instance.setParentTaskId(task.getParentTaskId());
		instance.setProcessDefinitionId(task.getProcessDefinitionId());
		instance.setProcessInstanceId(task.getProcessInstanceId());
		instance.setPriority(task.getPriority());
		instance.setSuspended(task.isSuspended());
		return instance;
	}

}
