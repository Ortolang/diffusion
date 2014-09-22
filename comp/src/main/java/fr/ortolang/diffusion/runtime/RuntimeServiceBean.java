package fr.ortolang.diffusion.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.process.ProcessDefinition;
import fr.ortolang.diffusion.runtime.process.ProcessState;
import fr.ortolang.diffusion.runtime.process.ProcessStep;
import fr.ortolang.diffusion.runtime.task.GreetTask;
import fr.ortolang.diffusion.runtime.task.HelloWorldTask;
import fr.ortolang.diffusion.runtime.task.ImportWorkspaceTask;
import fr.ortolang.diffusion.runtime.task.PublicationTask;
import fr.ortolang.diffusion.runtime.task.Task;
import fr.ortolang.diffusion.runtime.task.TaskState;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;


@Local(RuntimeService.class)
@Stateless(name = RuntimeService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class RuntimeServiceBean implements RuntimeService {

	private Logger logger = Logger.getLogger(RuntimeServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private NotificationService notification;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	@Resource
	private ManagedScheduledExecutorService executor;
	@Resource
	private ContextService contextService;
	
	private static Set<ProcessDefinition> definitions = new HashSet<ProcessDefinition> ();
	
	static {
		ProcessDefinition publication = new ProcessDefinition();
		publication.setName("simple-publication");
		publication.setDescription("A simple process for publish content without moderation (require to be moderator)");
		publication.addStep(new ProcessStep("publish-keys", PublicationTask.class));
		
		ProcessDefinition hello = new ProcessDefinition();
		hello.setName("helloworld");
		hello.setDescription("The famous hello world process !!");
		hello.addStep(new ProcessStep("hello", HelloWorldTask.class));
		hello.addStep(new ProcessStep("greet", GreetTask.class));
		
		ProcessDefinition workspace = new ProcessDefinition();
		workspace.setName("import-workspace");
		workspace.setDescription("Import a BagIt file as a complete workspace !!");
		workspace.addStep(new ProcessStep("import", ImportWorkspaceTask.class));
		
		definitions.add(publication);
		definitions.add(hello);
		definitions.add(workspace);
	}
	
	public RuntimeServiceBean() {
	}
	
	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registry = registryService;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notification = notificationService;
	}

	public MembershipService getMembershipService() {
		return membership;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
	}
	
	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
	public ManagedScheduledExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ManagedScheduledExecutorService executor) {
		this.executor = executor;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Set<ProcessDefinition> listProcessDefinitions() throws RuntimeServiceException {
		logger.log(Level.FINE, "listing process definitions");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			notification.throwEvent("", caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "list-definitions"), "");
			return definitions;
		} catch ( AuthorisationServiceException | AccessDeniedException | NotificationServiceException e ) {
			throw new RuntimeServiceException("unable to list process definitions", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createProcessInstance(String key, String name, String type, Map<String, String> params) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating process instance for key [" + key + "] and of type [" + type + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			
			findProcessDefinition(type);
			
			ProcessInstance process = new ProcessInstance();
			process.setId(UUID.randomUUID().toString());
			process.setName(name);
			process.setType(type);
			process.setInitier(caller);
			process.setState(ProcessState.PENDING);
			process.setCurrentStep(0);
			process.setParams(params);
			process.addLogEntry("Process created");
			em.persist(process);
			
			registry.register(key, process.getObjectIdentifier(), caller);
			
			authorisation.createPolicy(key, caller);
			
			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "create"), "");
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			logger.log(Level.WARNING, "an error occured during creating a process", e);
			ctx.setRollbackOnly();
			throw new RuntimeServiceException("unable to create process with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ProcessInstance readProcessInstance(String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.FINE, "reading process instance for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessInstance.OBJECT_TYPE);
			ProcessInstance process = em.find(ProcessInstance.class, identifier.getId());
			if (process == null) {
				throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
			}
			process.setKey(key);

			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "read"), "");
			return process;
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException e) {
			logger.log(Level.WARNING, "an error occured during reading a process", e);
			throw new RuntimeServiceException("unable to read the process with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public List<String> findProcessInstancesByInitier(String initier) throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding process instances for initier [" + initier + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(initier, subjects, "read");
			
			TypedQuery<ProcessInstance> query = em.createNamedQuery("findProcessInstancesForInitier", ProcessInstance.class).setParameter("initier", initier);
			List<ProcessInstance> processes = query.getResultList();
			List<String> results = new ArrayList<String> ();
			for ( ProcessInstance process : processes ) {
				String key = registry.lookup(process.getObjectIdentifier());
				results.add(key);
			}
			notification.throwEvent("", caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "find"), "initier=" + initier);
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
			throw new RuntimeServiceException("unable to find processes instances for initier [" + initier + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public List<String> findAllProcessInstances() throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding all process instances");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			
			TypedQuery<ProcessInstance> query = em.createNamedQuery("findAllProcessInstances", ProcessInstance.class);
			List<ProcessInstance> processes = query.getResultList();
			List<String> results = new ArrayList<String> ();
			for ( ProcessInstance process : processes ) {
				String key = registry.lookup(process.getObjectIdentifier());
				results.add(key);
			}
			notification.throwEvent("", caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "find-all"), "");
			return results;
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | IdentifierNotRegisteredException e) {
			throw new RuntimeServiceException("unable to find all processe instances", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.FINE, "starting execution of process instance for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessInstance.OBJECT_TYPE);
			ProcessInstance process = em.find(ProcessInstance.class, identifier.getId());
			if (process == null) {
				throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
			}
			if (!process.getState().equals(ProcessState.PENDING)) {
				throw new RuntimeServiceException("unable to start a process that is not in state " + ProcessState.PENDING);
			}
			ProcessDefinition definition = findProcessDefinition(process.getType());
			ProcessStep step = definition.getStep(process.getCurrentStep());
			if ( step == null ) {
				throw new RuntimeServiceException("unable to start a process that has no step");
			}
			
			try {
				String taskKey = UUID.randomUUID().toString(); 
				Task task = (Task) step.getTaskClass().newInstance();
				task.setKey(taskKey);
				task.setParams(process.getParams());
				task.setProcessKey(key);
				task.setProcessStep(process.getCurrentStep());
				Runnable ctxTask = contextService.createContextualProxy(task, Runnable.class);
				executor.schedule(ctxTask, 10, TimeUnit.SECONDS);
				
				process.setState(ProcessState.ACTIVE);
				process.putParam(ProcessInstance.PARAM_START,System.currentTimeMillis() + "");
				process.addLogEntry("Task created for step " + process.getCurrentStep() + " with key: " + taskKey);
			} catch (InstantiationException | IllegalAccessException e) {
				logger.log(Level.SEVERE, "unable to instanciate task", e);
				process.setState(ProcessState.ABORTED);
				process.putParam(ProcessInstance.PARAM_STOP,System.currentTimeMillis() + "");
				process.addLogEntry("Unable to create task for step " + process.getCurrentStep() + ": " + e.getMessage());
				process.addLogEntry("Process aborted");
			}
			em.merge(process);
			
			registry.update(key);
			
			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "start"), "");
		} catch (RegistryServiceException | NotificationServiceException e) {
			logger.log(Level.WARNING, "an error occured during starting process execution", e);
			throw new RuntimeServiceException("unable to start process execution for key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void abortProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.FINE, "aborting process instance execution for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessInstance.OBJECT_TYPE);
			ProcessInstance process = em.find(ProcessInstance.class, identifier.getId());
			if (process == null) {
				throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
			}
			if (process.getState().equals(ProcessState.ABORTED)) {
				throw new RuntimeServiceException("process is already in state " + ProcessState.ABORTED);
			}
			if (process.getState().equals(ProcessState.COMPLETED)) {
				throw new RuntimeServiceException("unable to abort a process that is in state " + ProcessState.COMPLETED);
			}
			process.setState(ProcessState.ABORTED);
			process.putParam(ProcessInstance.PARAM_STOP,System.currentTimeMillis() + "");
			process.addLogEntry("Process aborted");
			em.merge(process);
			
			registry.update(key);
			
			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "stop"), "");
		} catch (RegistryServiceException | NotificationServiceException e) {
			logger.log(Level.WARNING, "an error occured during stopping process execution", e);
			throw new RuntimeServiceException("unable to stop process execution for key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void notifyTaskExecution(Task task) throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.FINE, "notify task execution for process [" + task.getProcessKey() + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(task.getProcessKey());
			checkObjectType(identifier, ProcessInstance.OBJECT_TYPE);
			ProcessInstance process = em.find(ProcessInstance.class, identifier.getId());
			if (process == null) {
				throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
			}
			if (process.getState().equals(ProcessState.ABORTED)) {
				throw new RuntimeServiceException("unable to notify a process that is in state " + ProcessState.ABORTED);
			}
			if (process.getState().equals(ProcessState.COMPLETED)) {
				throw new RuntimeServiceException("unable to notify a process that is in state " + ProcessState.COMPLETED);
			}
			process.addLogEntry("notification received for task with key : " + task.getKey());
			process.addLogEntry(task.getLog());
			
			//TODO manage other task states...
			if ( task.getState().equals(TaskState.COMPLETED) ) {
				process.putAllParams(task.getParams());
				ProcessDefinition definition = findProcessDefinition(process.getType());
				ProcessStep step = definition.getStep(process.getCurrentStep()+1);
				if ( step == null ) {
					process.setState(ProcessState.COMPLETED);
					process.putParam(ProcessInstance.PARAM_STOP,System.currentTimeMillis() + "");
					process.addLogEntry("Process completed."); 
				} else {
					process.setCurrentStep(process.getCurrentStep()+1);
					try {
						String taskKey = UUID.randomUUID().toString(); 
						Task nextTask = (Task) step.getTaskClass().newInstance();
						nextTask.setKey(taskKey);
						nextTask.setParams(process.getParams());
						nextTask.setProcessKey(task.getProcessKey());
						nextTask.setProcessStep(process.getCurrentStep());
						Runnable ctxTask = contextService.createContextualProxy(nextTask, Runnable.class);
						executor.schedule(ctxTask, 10, TimeUnit.SECONDS);
						
						process.setState(ProcessState.ACTIVE);
						process.putParam(ProcessInstance.PARAM_START,System.currentTimeMillis() + "");
						process.addLogEntry("Task created for step " + process.getCurrentStep() + " with key: " + taskKey);
					} catch (InstantiationException | IllegalAccessException e) {
						logger.log(Level.SEVERE, "unable to instanciate next task", e);
						process.setState(ProcessState.ABORTED);
						process.putParam(ProcessInstance.PARAM_STOP,System.currentTimeMillis() + "");
						process.addLogEntry("Unable to create task for step " + process.getCurrentStep() + ": " + e.getMessage());
						process.addLogEntry("Process aborted");
					}
				}
			}
			if ( task.getState().equals(TaskState.ERROR) ) {
				process.setState(ProcessState.ABORTED);
				process.putParam(ProcessInstance.PARAM_STOP,System.currentTimeMillis() + "");
				process.addLogEntry("Task in error, process aborted");
			}
			em.merge(process);
			
			registry.update(task.getProcessKey());
			
			notification.throwEvent(task.getProcessKey(), caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "notify"), "");
		} catch (RegistryServiceException | NotificationServiceException e) {
			logger.log(Level.WARNING, "an error occured during notifying process execution", e);
			throw new RuntimeServiceException("unable to notify process execution of event for task with key [" + task.getKey() + "]", e);
		}
	}
	
	@Override
	public String getServiceName() {
		return RuntimeService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return RuntimeService.OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
			if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
				return OBJECT_PERMISSIONS_LIST[i][1].split(",");
			}
		}
		throw new OrtolangException("Unable to find object permissions list for object type : " + type);
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(RuntimeService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(ProcessInstance.OBJECT_TYPE)) {
				return readProcessInstance(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (RuntimeServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}
	
	private ProcessDefinition findProcessDefinition(String type) throws RuntimeServiceException {
		for ( ProcessDefinition definition : definitions ) {
			if ( definition.getName().equals(type) ) {
				return definition;
			}
		}
		throw new RuntimeServiceException("Unable to find a process definition for type " + type);
	}
	
	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws RuntimeServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new RuntimeServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new RuntimeServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}