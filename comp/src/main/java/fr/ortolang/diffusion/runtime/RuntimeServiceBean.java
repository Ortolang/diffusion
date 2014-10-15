package fr.ortolang.diffusion.runtime;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jgroups.util.UUID;

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
import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.entity.ProcessTask;
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
	@EJB
	private RuntimeEngine engine;
	@Resource
	private SessionContext ctx;
	@Resource
	private ManagedScheduledExecutorService executor;
	@Resource
	private ContextService contextService;
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createProcessDefinition(String key, InputStream definition)  throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "Creating process definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			
			String pdefid = engine.deployProcessDefinition(key + ".bpmn", definition);
			
			ProcessDefinition wdef = new ProcessDefinition();
			wdef.setId(pdefid);
			
			registry.register(key, wdef.getObjectIdentifier(), caller);
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "create"), "");
		} catch (AccessDeniedException e) {
			throw e;
		} catch (KeyAlreadyExistsException e) {
			ctx.setRollbackOnly();
			throw e;
		} catch (AuthorisationServiceException | RuntimeEngineException | RegistryServiceException | IdentifierAlreadyRegisteredException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new RuntimeServiceException("unable to create process definition", e);
		} 
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessDefinition> listProcessDefinitions() throws RuntimeServiceException {
		logger.log(Level.INFO, "Listing workflow definitions");
		try {
			List<ProcessDefinition> pdefs = engine.listProcessDefinitions();
			List<ProcessDefinition> epdefs = new ArrayList<ProcessDefinition> ();
			for ( ProcessDefinition pdef: pdefs ) {
				try {
					String key = registry.lookup(pdef.getObjectIdentifier());
					pdef.setKey(key);
					epdefs.add(pdef);
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.FINE, "unregistered process definition found in storage for id: " + pdef.getId());
				}
			}
			
			return epdefs;
		} catch ( RuntimeEngineException | RegistryServiceException e ) {
			throw new RuntimeServiceException("unable to list process defintions", e);
		} 
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessDefinition readProcessDefinition(String key)  throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "Reading process definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessDefinition.OBJECT_TYPE);
			ProcessDefinition def = engine.getProcessDefinition(identifier.getId());
			if ( def == null ) {
				throw new RuntimeServiceException("unable to find a process definition for id: " + identifier.getId());
			}
			def.setKey(key);
						
			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "read"), "");
			return def;
		} catch (RegistryServiceException | NotificationServiceException | RuntimeEngineException e) {
			throw new RuntimeServiceException("unable to get process definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] readProcessDefinitionModel(String key)  throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "Reading process definition model with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessDefinition.OBJECT_TYPE);
			byte[] model = engine.getProcessDefinitionModel(identifier.getId());
			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "read-model"), "");
			return model;
		} catch (RegistryServiceException | RuntimeEngineException | NotificationServiceException e) {
			throw new RuntimeServiceException("unable to get process definition model", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] readProcessDefinitionDiagram(String key)  throws RuntimeServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "reading process definition diagram for key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessDefinition.OBJECT_TYPE);
			byte[] diagram = engine.getProcessDefinitionModel(identifier.getId());
			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "read-diagram"), "");
			return diagram;
		} catch (RegistryServiceException | RuntimeEngineException | NotificationServiceException e) {
			throw new RuntimeServiceException("unable to get process definition diagram", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void suspendProcessDefinition(String key)  throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "Suspending process definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessDefinition.OBJECT_TYPE);
			engine.suspendProcessDefinition(identifier.getId());
			
			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "suspend"), "");
		} catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | RuntimeEngineException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new RuntimeServiceException("unable to suspend process definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessDefinition findProcessDefinitionByName(String name) throws RuntimeServiceException {
		logger.log(Level.INFO, "Searching process definition with name: " + name);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			ProcessDefinition def = engine.findLatestProcessDefinitionsForName(name);
			if ( def == null ) {
				throw new RuntimeServiceException("unable to find a process definition with name: " + name);
			}
			String key = registry.lookup(def.getObjectIdentifier());
			def.setKey(key);
						
			notification.throwEvent(key, caller, ProcessDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessDefinition.OBJECT_TYPE, "findByName"), "");
			return def;
		} catch (RegistryServiceException | RuntimeEngineException | IdentifierNotRegisteredException | NotificationServiceException e) {
			throw new RuntimeServiceException("unable to find process definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcessInstance(String key, String definition, Map<String, Object> variables) throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Starting process instance with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			
			ProcessDefinition def = engine.findLatestProcessDefinitionsForName(definition);
			if ( def == null ) {
				throw new RuntimeServiceException("unable to find a process definition with key: " + definition);
			}
			String dkey = registry.lookup(def.getObjectIdentifier());
			authorisation.checkPermission(dkey, subjects, "start");
			
			String bkey = UUID.randomUUID().toString();
			registry.register(key, new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, bkey), caller);
			authorisation.createPolicy(key, caller);

			variables.put(ProcessInstance.INITIER, caller);
			engine.startProcessInstance(def.getId(), bkey, variables);
			
			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "start"), "");
		} catch (Exception e) {
			throw new RuntimeServiceException("unable to start process Instance ", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessInstance> listProcessInstances(String initier, boolean active) throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Listing " + ((active)?"active":"all") + " process instances with " + ((initier!=null)?initier:"any") + " initier");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			if ( initier != null && initier.length() > 0 ) {
				authorisation.checkSuperUser(caller);
			} else {
				initier = caller;
			}
			
			List<ProcessInstance> allinstances = engine.listProcessInstances(initier, active);
			List<ProcessInstance> instances = new ArrayList<ProcessInstance> ();
			for (ProcessInstance instance : allinstances) {
				try {
					String ikey = registry.lookup(instance.getObjectIdentifier());
					instance.setKey(ikey);
					instances.add(instance);
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.FINE, "unregistered process instance found in storage for id: " + instance.getId());
				}
			}
			return instances;
		} catch ( RuntimeEngineException | RegistryServiceException | AuthorisationServiceException e ) {
			throw new RuntimeServiceException("unable to list process instances", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessInstance readProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "Reading process instance with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ProcessInstance.OBJECT_TYPE);
			ProcessInstance instance = engine.getProcessInstance(identifier.getId());
			if ( instance == null )  {
				throw new RuntimeServiceException("unable to find a process instance with id: " + identifier.getId());
			}
			instance.setKey(key);
						
			notification.throwEvent(key, caller, ProcessInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, "read"), "");
			return instance;
		} catch (Exception e) {
			throw new RuntimeServiceException("unable to read process instance", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessTask> listAllProcessTasks() throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Listing all process tasks");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			
			//TODO in case of registry mapping of tasks, perform reverse lookup
			List<ProcessTask> tasks = engine.listAllProcessTasks();
			return tasks;
		} catch (AuthorisationServiceException | RuntimeEngineException e) {
			throw new RuntimeServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessTask> listCandidateProcessTasks() throws RuntimeServiceException {
		logger.log(Level.INFO, "Listing candidate process tasks for connected user ");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> groups = membership.getProfileGroups(caller);
			
			//TODO in case of registry mapping of tasks, perform reverse lookup
			List<ProcessTask> tasks = new ArrayList<ProcessTask> ();
			tasks.addAll(engine.listCandidateGroupsProcessTasks(groups));
			tasks.addAll(engine.listCandidateProcessTasks(caller));
			return tasks;
		} catch (RuntimeEngineException | MembershipServiceException | KeyNotFoundException | AccessDeniedException e) {
			throw new RuntimeServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessTask> listAssignedProcessTasks() throws RuntimeServiceException {
		logger.log(Level.INFO, "Listing assigned process tasks for connected user ");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			//TODO in case of registry mapping of tasks, perform reverse lookup
			List<ProcessTask> tasks = engine.listAssignedProcessTasks(caller);
			return tasks;
		} catch (RuntimeEngineException e) {
			throw new RuntimeServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void claimProcessTask(String id) throws RuntimeServiceException {
		logger.log(Level.INFO, "Claiming process task with id: " + id);
		String caller = membership.getProfileKeyForConnectedIdentifier();
		try {
			engine.claimProcessTask(id, caller);
		} catch ( ActivitiObjectNotFoundException e ) {
			throw new RuntimeServiceException("unable to find a task with id: " + id);
		} catch ( ActivitiTaskAlreadyClaimedException e ) {
			throw new RuntimeServiceException("unable to claim the task with id: " + id + ", task already claimed by another user");
		} catch (RuntimeEngineException e) {
			throw new RuntimeServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void completeProcessTask(String id, Map<String, Object> params) throws RuntimeServiceException {
		logger.log(Level.INFO, "Completing task with id: " + id);
		try {
			engine.completeProcessTask(id, params);
		} catch ( ActivitiObjectNotFoundException e ) {
			throw new RuntimeServiceException("unable to find a task with id: " + id);
		} catch (RuntimeEngineException e) {
			throw new RuntimeServiceException(e);
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
		return new String[] {};
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(RuntimeService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(ProcessDefinition.OBJECT_TYPE)) {
				return readProcessDefinition(key);
			}
			
			if (identifier.getType().equals(ProcessInstance.OBJECT_TYPE)) {
				return readProcessInstance(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (RuntimeServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
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
