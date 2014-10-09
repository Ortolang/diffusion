package fr.ortolang.diffusion.workflow;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jgroups.util.UUID;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.workflow.engine.ProcessEngineService;
import fr.ortolang.diffusion.workflow.engine.ProcessEngineServiceException;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;
import fr.ortolang.diffusion.workflow.entity.WorkflowTask;


@Local(WorkflowService.class)
@Stateless(name = WorkflowService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class WorkflowServiceBean implements WorkflowService {
	
	private Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private NotificationService notification;
	@EJB
	private ProcessEngineService engine;
	@Resource
	private SessionContext ctx;
	@Resource
	private ManagedScheduledExecutorService executor;
	@Resource
	private ContextService contextService;
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createWorkflowDefinition(String key, InputStream definition)  throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Creating workflow definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			
			Deployment deploy = engine.getRepositoryService().createDeployment().addInputStream(key + ".bpmn", definition).enableDuplicateFiltering().deploy();
			ProcessDefinition pdef = engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();
			
			WorkflowDefinition wdef = new WorkflowDefinition();
			wdef.setId(pdef.getId());
			
			registry.register(key, wdef.getObjectIdentifier(), caller);
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "create"), "");
		} catch (AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			ctx.setRollbackOnly();
			throw new WorkflowServiceException("unable to create Workfow Definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowDefinition> listWorkflowDefinitions() throws WorkflowServiceException {
		logger.log(Level.INFO, "Listing workflow definitions");
		try {
			List<ProcessDefinition> pdefs = engine.getRepositoryService().createProcessDefinitionQuery().list();
			logger.log(Level.FINE, pdefs.size() +  " workflow definitions found");
			List<WorkflowDefinition> defs = new ArrayList<WorkflowDefinition> ();
			for (ProcessDefinition pdef : pdefs) {
				WorkflowDefinition wdef = WorkflowDefinition.fromProcessDefinition(pdef);
				try {
					String key = registry.lookup(wdef.getObjectIdentifier());
					logger.log(Level.FINE, "registry entry found for workflow definition with id: " + wdef.getId());
					wdef.setKey(key);
					defs.add(wdef);
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.WARNING, "workflow definition id found but no registry entry associated for this id: " + wdef.getId());
					//TODO maybe suspend this workflow definition !!
				}
			}
			return defs;
		} catch ( ProcessEngineServiceException | RegistryServiceException e ) {
			throw new WorkflowServiceException("unable to list Workflow Defintions", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public WorkflowDefinition getWorkflowDefinition(String key)  throws WorkflowServiceException {
		logger.log(Level.INFO, "Getting workflow definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, WorkflowDefinition.OBJECT_TYPE);
			ProcessDefinition def = engine.getRepositoryService().getProcessDefinition(identifier.getId());
			if ( def == null ) {
				throw new WorkflowServiceException("unable to find a Workflow Definition with key: " + key);
			}
			
			WorkflowDefinition wdef = new WorkflowDefinition();
			wdef.setKey(key);
			wdef.setId(def.getId());
			wdef.setName(def.getName());
			wdef.setDescription(def.getDescription());
			wdef.setSuspended(def.isSuspended());
			wdef.setVersion(def.getVersion());
						
			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "read"), "");
			
			return wdef;
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to get Workfow Definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] getWorkflowDefinitionModel(String key)  throws WorkflowServiceException {
		logger.log(Level.INFO, "Getting workflow definition model with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, WorkflowDefinition.OBJECT_TYPE);
			InputStream model = engine.getRepositoryService().getProcessModel(identifier.getId());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				IOUtils.copy(model, baos);
			} finally {
				IOUtils.closeQuietly(model);
			}
			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "read-model"), "");
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to get Workfow Definition Model", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public byte[] getWorkflowDefinitionDiagram(String key)  throws WorkflowServiceException {
		logger.log(Level.INFO, "Getting workflow definition diagram for key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, WorkflowDefinition.OBJECT_TYPE);
			InputStream diagram = engine.getRepositoryService().getProcessDiagram(identifier.getId());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				IOUtils.copy(diagram, baos);
			} finally {
				IOUtils.closeQuietly(diagram);
			}
			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "read-diagram"), "");
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to get Workfow Definition Diagram", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void suspendWorkflowDefinition(String key)  throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Suspending workflow definition with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, WorkflowDefinition.OBJECT_TYPE);
			engine.getRepositoryService().suspendProcessDefinitionById(identifier.getId());
			
			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "suspend"), "");
		} catch (AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			ctx.setRollbackOnly();
			throw new WorkflowServiceException("unable to suspend Workfow Definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public WorkflowDefinition findWorkflowDefinitionForName(String name) throws WorkflowServiceException {
		logger.log(Level.INFO, "Finding Workflow Definition with name: " + name);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			ProcessDefinition def = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(name).latestVersion().singleResult();
			if ( def == null ) {
				throw new WorkflowServiceException("unable to find a Workflow Definition with key: " + name);
			}
			
			WorkflowDefinition wdef = new WorkflowDefinition();
			wdef.setId(def.getId());
			wdef.setName(def.getName());
			wdef.setDescription(def.getDescription());
			wdef.setSuspended(def.isSuspended());
			wdef.setVersion(def.getVersion());
			
			String key = registry.lookup(wdef.getObjectIdentifier());
			wdef.setKey(key);
						
			notification.throwEvent(key, caller, WorkflowDefinition.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowDefinition.OBJECT_TYPE, "findByName"), "");
			return wdef;
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to find Workfow Definition", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createWorkflowInstance(String key, String definition, Map<String, Object> params) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Creating workflow instance with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			params.put(WorkflowInstance.INITIER, caller);
			
			ProcessDefinition def = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(definition).latestVersion().singleResult();
			if ( def == null ) {
				throw new WorkflowServiceException("unable to find a Workflow Definition with key: " + definition);
			}
			WorkflowDefinition wdef = new WorkflowDefinition();
			wdef.setId(def.getId());
			String dkey = registry.lookup(wdef.getObjectIdentifier());
			authorisation.checkPermission(dkey, subjects, "start");
			
			String bkey = UUID.randomUUID().toString();
			WorkflowInstance winst = new WorkflowInstance();
			winst.setKey(key);
			winst.setId(bkey);
			winst.setParams(params);
			winst.setDefinitionId(def.getId());
			
			registry.register(key, winst.getObjectIdentifier(), caller);
			authorisation.createPolicy(key, caller);

			WorkflowInstanceRunner runner = new WorkflowInstanceRunner(engine.getRuntimeService(), winst);
			Runnable ctxRunner = contextService.createContextualProxy(runner, Runnable.class);
			executor.schedule(ctxRunner, 3, TimeUnit.SECONDS);
			
			notification.throwEvent(key, caller, WorkflowInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowInstance.OBJECT_TYPE, "create"), "");
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to create Workfow Instance ", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public WorkflowInstance getWorkflowInstance(String key) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Getting workflow instance with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, WorkflowInstance.OBJECT_TYPE);
			ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(identifier.getId()).singleResult();
			if ( instance == null )  {
				throw new WorkflowServiceException("unable to find a process instance with id: " + identifier.getId());
			}
			
			WorkflowInstance winst = WorkflowInstance.fromProcessInstance(instance);
			winst.setKey(key);
						
			notification.throwEvent(key, caller, WorkflowInstance.OBJECT_TYPE, OrtolangEvent.buildEventType(WorkflowService.SERVICE_NAME, WorkflowInstance.OBJECT_TYPE, "read"), "");
			return winst;
		} catch (Exception e) {
			throw new WorkflowServiceException("unable to get Workfow Instance", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowInstance> listWorkflowInstances(String initier, String definition, boolean activeOnly) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Listing " + ((activeOnly)?"active":"all") + " workflow instances of " 
					+ ((definition!=null)?definition:"all") + " definitions with " + ((initier!=null)?initier:"any") + " initier");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			List<WorkflowInstance> instances = new ArrayList<WorkflowInstance> ();

			ProcessInstanceQuery query = engine.getRuntimeService().createProcessInstanceQuery().includeProcessVariables();
			if ( initier != null && initier.length() > 0 ) {
				authorisation.checkSuperUser(caller);
				query.variableValueEquals(WorkflowInstance.INITIER, initier);
			} else {
				query.variableValueEquals(WorkflowInstance.INITIER, caller);
			}
			if ( definition != null && definition.length() > 0 ) {
				OrtolangObjectIdentifier didentifier = registry.lookup(definition);
				checkObjectType(didentifier, WorkflowDefinition.OBJECT_TYPE);
				ProcessDefinition def = engine.getRepositoryService().getProcessDefinition(didentifier.getId());
				if ( def == null )  {
					throw new WorkflowServiceException("unable to find a Workflow Definition for id: " + didentifier.getId());
				}
				query.processDefinitionKey(didentifier.getId());
			}
			if ( activeOnly ) {
				query.active();
			}
			List<ProcessInstance> pinstances = query.list();
			logger.log(Level.FINE, pinstances.size() + " process instances.");
			for (ProcessInstance instance : pinstances) {
				WorkflowInstance winstance = WorkflowInstance.fromProcessInstance(instance);
				try {
					String wikey = registry.lookup(winstance.getObjectIdentifier());
					winstance.setKey(wikey);
					instances.add(winstance);
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.WARNING, "Workflow Instance found but no registry key associated with it's identifier : " + winstance.getObjectIdentifier());
				}
			}
			return instances;
		} catch ( ProcessEngineServiceException | RegistryServiceException | KeyNotFoundException | MembershipServiceException | AuthorisationServiceException e ) {
			throw new WorkflowServiceException("unable to list Workflow Instances", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowTask> listWorkflowTasks() throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Listing all tasks for connected user ");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			
			List<WorkflowTask> tasks = new ArrayList<WorkflowTask> ();
			TaskQuery query = engine.getTaskService().createTaskQuery();
			List<Task> ptasks = query.list();
			logger.log(Level.INFO, "Found " + ptasks.size() + " tasks.");
			for (Task task : ptasks) {
				tasks.add(WorkflowTask.fromTask(task));
			}
			return tasks;
		} catch (Exception e) {
			throw new WorkflowServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<WorkflowTask> listCandidateWorkflowTasks() throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Listing candidate tasks for connected user ");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> groups = membership.getProfileGroups(caller);
			List<WorkflowTask> tasks = new ArrayList<WorkflowTask> ();
			TaskQuery query = engine.getTaskService().createTaskQuery();
			query.taskCandidateGroupIn(groups);
			List<Task> ptasks = query.list();
			logger.log(Level.INFO, "Found " + ptasks.size() + " tasks.");
			for (Task task : ptasks) {
				tasks.add(WorkflowTask.fromTask(task));
			}
			return tasks;
		} catch (Exception e) {
			throw new WorkflowServiceException(e);
		}
	}

//	@Override
//	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
//	public List<WorkflowTask> listAssignedWorkflowTasks() throws WorkflowServiceException, AccessDeniedException {
//		logger.log(Level.INFO, "Listing assigned tasks for connected user ");
//		try {
//			String caller = membership.getProfileKeyForConnectedIdentifier();
//			List<WorkflowTask> tasks = new ArrayList<WorkflowTask> ();
//			TaskQuery query = engine.getTaskService().createTaskQuery();
//			query.taskAssignee(caller);
//			List<Task> ptasks = query.list();
//			logger.log(Level.INFO, "Found " + ptasks.size() + " tasks.");
//			for (Task task : ptasks) {
//				tasks.add(WorkflowTask.fromTask(task));
//			}
//			return tasks;
//		} catch (Exception e) {
//			throw new WorkflowServiceException(e);
//		}
//	}
//	


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
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(WorkflowService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(WorkflowDefinition.OBJECT_TYPE)) {
				return getWorkflowDefinition(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (WorkflowServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}
	
	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws MembershipServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new MembershipServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new MembershipServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}
