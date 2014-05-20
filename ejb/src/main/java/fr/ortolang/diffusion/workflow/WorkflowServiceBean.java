package fr.ortolang.diffusion.workflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
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
import fr.ortolang.diffusion.OrtolangObjectProperty;
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
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.workflow.entity.Process;
import fr.ortolang.diffusion.workflow.process.ProcessDefinition;
import fr.ortolang.diffusion.workflow.process.ProcessExecutionStatus;
import fr.ortolang.diffusion.workflow.process.ProcessExecutionTask;
import fr.ortolang.diffusion.workflow.process.SimplePublicationProcess;


@Remote(WorkflowService.class)
@Local(WorkflowServiceLocal.class)
@Stateless(name = WorkflowService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class WorkflowServiceBean implements WorkflowService, WorkflowServiceLocal {

	private Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
	
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
	
	private Set<ProcessDefinition> definitions = new HashSet<ProcessDefinition> ();
	
	public WorkflowServiceBean() {
	}
	
	@PostConstruct
	public void loadDefinitions() {
		definitions.add(new ProcessDefinition("simple-publication", "A simple process for publish content without moderation (require to be moderator)", SimplePublicationProcess.class.getName()));
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
	public void createProcess(String key, String name, String type, Map<String, String> params) throws WorkflowServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating process for key [" + key + "] and type [" + type + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			
			Process process = new Process();
			process.setId(UUID.randomUUID().toString());
			process.setName(name);
			process.setType(type);
			process.setInitier(caller);
			process.setStatus(ProcessExecutionStatus.WAITING.name());
			process.setLog("");
			em.persist(process);
			
			registry.register(key, process.getObjectIdentifier());
			registry.setProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			registry.setProperty(key, OrtolangObjectProperty.AUTHOR, caller);
			
			authorisation.createPolicy(key, caller);
			
			ProcessDefinition definition = findProcessDefinition(type);
			ProcessExecutionTask task = (ProcessExecutionTask) Class.forName(definition.getClazz()).newInstance();
			Runnable ctxTask = contextService.createContextualProxy(task, Runnable.class);
			task.setProcessKey(key);
			task.setParams(params);
			task.setCaller(caller);
			executor.schedule(ctxTask, 5, TimeUnit.SECONDS);
			
			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "create"), "");
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | KeyNotFoundException | AuthorisationServiceException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			logger.log(Level.WARNING, "an error occured during creating a process", e);
			ctx.setRollbackOnly();
			throw new WorkflowServiceException("unable to create process with key [" + key + "]", e);
		}
	}
	
	@Override
	public Process readProcess(String key) throws WorkflowServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.FINE, "reading process for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process process = em.find(Process.class, identifier.getId());
			if (process == null) {
				throw new WorkflowServiceException("unable to find a process for id " + identifier.getId());
			}
			process.setKey(key);

			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "read"), "");
			return process;
		} catch (RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException e) {
			logger.log(Level.WARNING, "an error occured during reading a process", e);
			throw new WorkflowServiceException("unable to read the process with key [" + key + "]", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> findProcessForInitier(String initier) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.FINE, "finding processes for initier [" + initier + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(initier, subjects, "read");
			
			TypedQuery<Process> query = em.createNamedQuery("findProcessForInitier", Process.class).setParameter("initier", initier);
			List<Process> processes = query.getResultList();
			List<String> results = new ArrayList<String> ();
			for ( Process process : processes ) {
				String key = registry.lookup(process.getObjectIdentifier());
				results.add(key);
			}
			notification.throwEvent("", caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "find"), "initier=" + initier);
			return results;
		} catch (NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
			throw new WorkflowServiceException("unable to find processes for initier [" + initier + "]", e);
		}
	}

	@Override
	public Set<ProcessDefinition> listProcessDefinitions() throws WorkflowServiceException {
		logger.log(Level.FINE, "listing process definitions");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			notification.throwEvent("", caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "list-definitions"), "");
			return definitions;
		} catch ( AuthorisationServiceException | AccessDeniedException | NotificationServiceException e ) {
			throw new WorkflowServiceException("unable to list process definitions", e);
		}
	}
	
	@Override
	public void startProcessExecution(String key) throws WorkflowServiceException {
		logger.log(Level.FINE, "starting process execution for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process process = em.find(Process.class, identifier.getId());
			if (process == null) {
				throw new WorkflowServiceException("unable to find a process for id " + identifier.getId());
			}
			process.setStart(System.currentTimeMillis() + "");
			process.setStatus(ProcessExecutionStatus.RUNNING.name());
			em.merge(process);

			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "start"), "");
		} catch (RegistryServiceException | NotificationServiceException | KeyNotFoundException e) {
			logger.log(Level.WARNING, "an error occured during starting process execution", e);
			throw new WorkflowServiceException("unable to start process execution for key [" + key + "]", e);
		}
	}
	
	@Override
	public void stopProcessExecution(String key, int completed) throws WorkflowServiceException {
		logger.log(Level.FINE, "stopping process execution for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process process = em.find(Process.class, identifier.getId());
			if (process == null) {
				throw new WorkflowServiceException("unable to find a process for id " + identifier.getId());
			}
			process.setStop(System.currentTimeMillis() + "");
			process.setStatus(ProcessExecutionStatus.STOPPED.name());
			em.merge(process);

			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "stop"), "");
		} catch (RegistryServiceException | NotificationServiceException | KeyNotFoundException e) {
			logger.log(Level.WARNING, "an error occured during stopping process execution", e);
			throw new WorkflowServiceException("unable to stop process execution for key [" + key + "]", e);
		}
	}
	
	@Override
	public void addProcessExecutionLog(String key, String logentry) throws WorkflowServiceException {
		logger.log(Level.FINE, "stopping process execution for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process process = em.find(Process.class, identifier.getId());
			if (process == null) {
				throw new WorkflowServiceException("unable to find a process for id " + identifier.getId());
			}
			process.addLogEntry(logentry);
			em.merge(process);

			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, "log"), "");
		} catch (RegistryServiceException | NotificationServiceException | KeyNotFoundException e) {
			logger.log(Level.WARNING, "an error occured during logging process execution", e);
			throw new WorkflowServiceException("unable to log process execution for key [" + key + "]", e);
		}
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
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}
	
	private ProcessDefinition findProcessDefinition(String type) throws WorkflowServiceException {
		for ( ProcessDefinition definition : definitions ) {
			if ( definition.getName().equals(type) ) {
				return definition;
			}
		}
		throw new WorkflowServiceException("Unable to find a process definition for type " + type);
	}
	
	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws WorkflowServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new WorkflowServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new WorkflowServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}