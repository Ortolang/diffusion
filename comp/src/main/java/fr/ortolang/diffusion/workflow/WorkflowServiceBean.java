package fr.ortolang.diffusion.workflow;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;


@Startup
@Local(WorkflowService.class)
@Singleton(name = WorkflowService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class WorkflowServiceBean implements WorkflowService {
	
	private Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
	
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@Resource
	private SessionContext ctx;
	@Resource
	private ManagedExecutorService executor;
	@PersistenceUnit(unitName = "ortolangPU")
	private EntityManagerFactory emf;
	
	
	
	private ProcessEngine engine;

	@PostConstruct
	public void init() {
		logger.log(Level.INFO, "Initilizing WorkflowServiceBean");
		if (engine == null) {
			ProcessEngineConfiguration config = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
			config.setJpaHandleTransaction(false);
			config.setJpaCloseEntityManager(false);
			config.setJpaEntityManagerFactory(emf);
			config.setDatabaseSchemaUpdate("true");
			config.setMailSessionJndi("java:jboss/mail/Default");
			WorkflowJobExecutor jobExecutor = new WorkflowJobExecutor(executor);
			config.setJobExecutor(jobExecutor);
			engine = config.buildProcessEngine();
			logger.log(Level.INFO, "ProcessEngine created: " + engine.getName());
		}
		logger.log(Level.INFO, "WorkflowServiceBean initialized");
	}

	@PreDestroy
	public void dispose() {
		logger.log(Level.INFO, "Stopping WorkflowServiceBean");
		if (engine != null) {
			engine.close();
		}
		logger.log(Level.INFO, "WorkflowServiceBean stopped");
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String deployWorkflowDefinition(String name, InputStream definition)  throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Deploying workflow definition");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			RepositoryService repository = engine.getRepositoryService();
			String nname = name;
			if ( !nname.endsWith(".bpmn") ) {
				nname += ".bpmn";
			}
			Deployment deploy = repository.createDeployment().addInputStream(nname, definition).enableDuplicateFiltering().deploy();
			logger.log(Level.INFO, "Deployment done with id : " + deploy.getId());
			return deploy.getId();
		} catch (AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			throw new WorkflowServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowDefinition> listWorkflowDefinitions() throws WorkflowServiceException {
		logger.log(Level.INFO, "Listing workflow definitions");
		RepositoryService repository = engine.getRepositoryService();
		List<ProcessDefinition> pdefs = repository.createProcessDefinitionQuery().list();
		List<WorkflowDefinition> workflows = new ArrayList<WorkflowDefinition> ();
		for (ProcessDefinition def : pdefs) {
			logger.log(Level.INFO, "found process definition: " + def.getId());
			workflows.add(WorkflowDefinition.fromProcessDefinition(def));
		}
		return workflows;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String startWorkflowInstance(String definition, Map<String, Object> params) throws WorkflowServiceException {
		logger.log(Level.INFO, "Starting new workflow instance with definition: " + definition);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			params.put("initier", caller);
			
			RepositoryService repository = engine.getRepositoryService();
			List<ProcessDefinition> defs = repository.createProcessDefinitionQuery().processDefinitionKey(definition).active().list();
			if ( defs.isEmpty() ) {
				throw new WorkflowServiceException("unable to find an active workflow definition with key: " + definition);
			}
			RuntimeService runtime = engine.getRuntimeService();
			String bkey = caller + "/" + UUID.randomUUID().toString();
			ProcessInstance instance = runtime.startProcessInstanceByKey(definition, bkey, params);
			logger.log(Level.INFO, "Process started: " + instance.getId());
			return instance.getId();
		} catch (Exception e) {
			throw new WorkflowServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowInstance> listWorkflowInstances(String definition, boolean activeOnly) throws WorkflowServiceException {
		logger.log(Level.INFO, "Trying to list " + ((activeOnly)?"active":"all") + " workflow instances of " + ((definition!=null)?definition:"all") + " definitions");
		List<WorkflowInstance> instances = new ArrayList<WorkflowInstance> ();
		RuntimeService runtime = engine.getRuntimeService();
		ProcessInstanceQuery query = runtime.createProcessInstanceQuery();
		if ( definition != null && definition.length() > 0 ) {
			query.processDefinitionKey(definition);
		}
		if ( activeOnly ) {
			query.active();
		}
		List<ProcessInstance> pinstances = query.list();
		logger.log(Level.INFO, "Found " + pinstances.size() + " process isntances.");
		for (ProcessInstance instance : pinstances) {
			instances.add(WorkflowInstance.fromProcessInstance(instance));
		}
		return instances;
	}

	@Override
	public String getServiceName() {
		return WorkflowService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return WorkflowService.OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		return new String[] {};
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		throw new OrtolangException("This service does not manage any object");
	}

}
