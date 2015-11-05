package fr.ortolang.diffusion.runtime;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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

import org.activiti.engine.task.IdentityLink;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngine;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
import fr.ortolang.diffusion.runtime.entity.RemoteProcess;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;


@Local(RuntimeService.class)
@Stateless(name = RuntimeService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class RuntimeServiceBean implements RuntimeService {
	
	private static final Logger LOGGER = Logger.getLogger(RuntimeServiceBean.class.getName());
	public static final String DEFAULT_RUNTIME_HOME = "/runtime";
	
	private static final String TRACE_FILE_EXTENSION = ".log";
	private static final String[] OBJECT_TYPE_LIST = new String[] { Process.OBJECT_TYPE };
	private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
			{ Process.OBJECT_TYPE, "read,update,delete,start,abort" } };
	
	private static Path base;
    
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
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	@Resource
	private ManagedScheduledExecutorService executor;
	@Resource
	private ContextService contextService;
	
	public RuntimeServiceBean() {
    }

    public Path getBase() throws RuntimeServiceException {
        if ( base == null ) {
            try {
                base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_RUNTIME_HOME);
                Files.createDirectories(base);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to build base working directory for runtime service", e);
                throw new RuntimeServiceException("unable to build base working directory for runtime service", e);
            }
        }
        return base;
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void importProcessTypes() throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Importing configured process types");
		try {
			String[] types = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.RUNTIME_DEFINITIONS).split(",");
			engine.deployDefinitions(types);
		} catch (RuntimeEngineException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while importing process types", e);
			throw new RuntimeServiceException("unable to import configured process types", e);
		} 
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessType> listProcessTypes() throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Listing process types");
		try {
			return engine.listProcessTypes();
		} catch (RuntimeEngineException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while listing process types", e);
			throw new RuntimeServiceException("unable to list process types", e);
		} 
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Process createProcess(String key, String type, String name, String workspace) throws RuntimeServiceException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Creating new process of type: " + type);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			ProcessType ptype = engine.getProcessTypeByKey(type);
			if ( ptype == null ) {
				throw new RuntimeServiceException("unable to find a process type named: " + type);
			}
			
			String id = UUID.randomUUID().toString();
			Process process = new Process();
			process.setId(id);
			process.setInitier(caller);
			process.setKey(key);
			process.setName(name);
			process.setWorkspace(workspace);
			process.setType(type);
			process.setState(State.PENDING);
			process.appendLog(new Date() + "  PROCESS CREATED BY " + caller);
			em.persist(process);
			
			registry.register(key, new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, id), caller);
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "create"));
			return process;
		} catch (RuntimeEngineException | RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while creating process", e);
			throw new RuntimeServiceException("unable to create process ", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcess(String key, Map<String, Object> variables) throws RuntimeServiceException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Starting process with key: " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "start");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process process = em.find(Process.class, identifier.getId());
			if (process == null) {
				throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
			}
			if ( !process.getState().equals(State.PENDING) ) {
				throw new RuntimeServiceException("unable to start process, state is not " + State.PENDING);
			}
			process.setKey(key);
			process.appendLog(new Date() + "  PROCESS STATE CHANGED TO " + State.SUBMITTED + " BY " + caller);
			process.setState(State.SUBMITTED);
			process.setStart(System.currentTimeMillis());
			em.persist(process);
			
			variables.put(Process.INITIER_VAR_NAME, process.getInitier());
			if ( process.getWorkspace() != null && process.getWorkspace().length() > 0 ) {
			    variables.put(Process.WSKEY_VAR_NAME, process.getWorkspace());
			}
			
			engine.startProcess(process.getType(), process.getId(), variables);
			
			registry.update(key);
			notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "start"));
		} catch (KeyLockedException | MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | RegistryServiceException | RuntimeEngineException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while submitting process for start", e);
			throw new RuntimeServiceException("unable to submit process for start", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void abortProcess(String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
	    LOGGER.log(Level.INFO, "Killing process with key: " + key);
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "abort");
            
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Process.OBJECT_TYPE);
            Process process = em.find(Process.class, identifier.getId());
            if (process == null) {
                throw new RuntimeServiceException("unable to find a process for id " + identifier.getId());
            }
            if ( !process.getState().equals(State.RUNNING) ) {
                throw new RuntimeServiceException("unable to kill process, state is not " + State.RUNNING);
            }
            process.setKey(key);
            process.appendLog(new Date() + "  PROCESS STATE CHANGED TO " + State.ABORTED + " BY " + caller);
            process.setState(State.ABORTED);
            process.setStop(System.currentTimeMillis());
            em.persist(process);
            
            engine.deleteProcess(process.getId());
            
            registry.update(key);
            notification.throwEvent(key, caller, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "abort"));
        } catch (KeyLockedException | MembershipServiceException | AuthorisationServiceException | RegistryServiceException | RuntimeEngineException | NotificationServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while killing process", e);
            throw new RuntimeServiceException("unable to kill process", e);
        }
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Process> listCallerProcesses(State state) throws RuntimeServiceException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Listing caller processes in " + ((state != null)?"state="+state:"all states"));
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			TypedQuery<Process> query;
			if ( state != null ) {
				query = em.createNamedQuery("findProcessByIniterAndState", Process.class).setParameter("state", state).setParameter("initier", caller);
			} else {
				query = em.createNamedQuery("findProcessByInitier", Process.class).setParameter("initier", caller);
			}
			
			List<Process> rprocesses = new ArrayList<Process>();
			for (Process process : query.getResultList()) {
				try {
					String ikey = registry.lookup(process.getObjectIdentifier());
					process.setKey(ikey);
					rprocesses.add(process);
				} catch ( IdentifierNotRegisteredException e ) {
					LOGGER.log(Level.WARNING, "unregistered process found in storage for id: " + process.getId());
				}
			}
			return rprocesses;
		} catch ( RegistryServiceException e ) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while listing processes", e);
			throw new RuntimeServiceException("unable to list processes", e);
		}
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Process> listWorkspaceProcesses(String wskey, State state) throws RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "Listing workspace processes in " + ((state != null)?"state="+state:"all states"));
        try {
            TypedQuery<Process> query;
            if ( state != null ) {
                query = em.createNamedQuery("findProcessByWorkspaceAndState", Process.class).setParameter("state", state).setParameter("workspace", wskey);
            } else {
                query = em.createNamedQuery("findProcessByWorkspace", Process.class).setParameter("workspace", wskey);
            }
            
            List<Process> rprocesses = new ArrayList<Process>();
            for (Process process : query.getResultList()) {
                try {
                    String ikey = registry.lookup(process.getObjectIdentifier());
                    process.setKey(ikey);
                    rprocesses.add(process);
                } catch ( IdentifierNotRegisteredException e ) {
                    LOGGER.log(Level.WARNING, "unregistered process found in storage for id: " + process.getId());
                }
            }
            return rprocesses;
        } catch ( RegistryServiceException e ) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while listing processes", e);
            throw new RuntimeServiceException("unable to list processes", e);
        }
    }
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Process readProcess(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Reading process with key: " + key);
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Process.OBJECT_TYPE);
			Process instance = em.find(Process.class, identifier.getId());
			if ( instance == null )  {
				throw new RuntimeServiceException("unable to find a process with id: " + identifier.getId());
			}
			instance.setKey(key);

			return instance;
		} catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while reading process", e);
			throw new RuntimeServiceException("unable to read process", e);
		}
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public File readProcessTrace(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "Reading trace of process with key: " + key);
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Process.OBJECT_TYPE);
            Process instance = em.find(Process.class, identifier.getId());
            if ( instance == null )  {
                throw new RuntimeServiceException("unable to find a process with id: " + identifier.getId());
            }
            Path trace = Paths.get(getBase().toString(), instance.getId() + TRACE_FILE_EXTENSION);
            if ( Files.exists(trace) ) {
                return trace.toFile();
            } else {
                throw new RuntimeServiceException("unable to find trace file for process with key: " + key);
            }
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading process", e);
            throw new RuntimeServiceException("unable to read process", e);
        }
    }
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateProcessState(String pid, State state) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Updating state of process with pid: " + pid);
		try {
			Process process = em.find(Process.class, pid);
			if ( process == null )  {
				throw new RuntimeServiceException("unable to find a process with id: " + pid);
			}
			process.setState(state);
			if(state.equals(State.ABORTED) || state.equals(State.COMPLETED) || state.equals(State.SUSPENDED)) {
				process.setStop(System.currentTimeMillis());
			}
			process.appendLog(new Date() + "  PROCESS STATE CHANGED TO " + state);
			em.merge(process);
			
			String key = registry.lookup(process.getObjectIdentifier());
			registry.update(key);
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("state", state.name());
			notification.throwEvent(key, RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "change-state"), argumentsBuilder.build());
		} catch (Exception e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while updating process state", e);
			throw new RuntimeServiceException("unable to update process state", e);
		}
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void appendProcessLog(String pid, String log) throws RuntimeServiceException {
        LOGGER.log(Level.INFO, "Appending log to process with pid: " + pid);
        try {
            Process process = em.find(Process.class, pid);
            if ( process == null )  {
                throw new RuntimeServiceException("unable to find a process with id: " + pid);
            }
            process.appendLog(log);
            em.merge(process);
            
            String key = registry.lookup(process.getObjectIdentifier());
            registry.update(key);
            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("message", log);
            notification.throwEvent(key, RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "log-info"), argumentsBuilder.build());
        } catch (Exception e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while appending log to process", e);
            throw new RuntimeServiceException("unable to append process log", e);
        }
    }
		
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void appendProcessTrace(String pid, String trace) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Appending trace to process with pid: " + pid);
		try {
		    Path logfile = Paths.get(getBase().toString(), pid + TRACE_FILE_EXTENSION);
		    if ( !Files.exists(logfile) ) {
		        String head = "## TRACE FILE FOR PROCESS WITH PID: " + pid + "\r\n##\r\n\r\n";
		        Files.write(logfile, head.getBytes(), StandardOpenOption.CREATE);
		    }
		    Files.write(logfile, trace.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while appending trace to process", e);
			throw new RuntimeServiceException("unable to append process trace", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateProcessActivity(String pid, String name) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Updating activity of process with pid: " + pid);
		try {
			Process process = em.find(Process.class, pid);
			if ( process == null )  {
				throw new RuntimeServiceException("unable to find a process with id: " + pid);
			}
			process.setActivity(name);
			em.merge(process);
			
			String key = registry.lookup(process.getObjectIdentifier());
			registry.update(key);
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("activity", name);
			notification.throwEvent(key, RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, "change-activity"), argumentsBuilder.build());
		} catch (Exception e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while updating process activity", e);
			throw new RuntimeServiceException("unable to update process activity", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void pushTaskEvent(String pid, Set<IdentityLink> candidates, RuntimeEngineEvent.Type type) {
        ArgumentsBuilder argumentsBuilder;
        String eventName = type.name().substring(type.name().lastIndexOf("_") + 1).toLowerCase();
        for (IdentityLink candidate : candidates) {
            try {
                if (candidate.getUserId() != null) {
                    LOGGER.log(Level.INFO, "USER CANDIDATE");
                    argumentsBuilder = new ArgumentsBuilder("user", candidate.getUserId());
                    notification.throwEvent(pid, RuntimeService.SERVICE_NAME, HumanTask.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, HumanTask.OBJECT_TYPE, eventName), argumentsBuilder.build());
                } else if (candidate.getGroupId() != null) {
                    LOGGER.log(Level.INFO, "GROUP CANDIDATE");
                    argumentsBuilder = new ArgumentsBuilder("group", candidate.getGroupId());
                    notification.throwEvent(pid, RuntimeService.SERVICE_NAME, HumanTask.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, HumanTask.OBJECT_TYPE, eventName), argumentsBuilder.build());
                }
            } catch (NotificationServiceException e) {
                LOGGER.log(Level.SEVERE, "unexpected error occurred while pushing task event", e);
            }
        }
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listCandidateTasks() throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Listing candidate tasks");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> groups = membership.getProfileGroups(caller);

			return engine.listCandidateTasks(caller, groups);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while listing candidate tasks", e);
			throw new RuntimeServiceException("unable to list candidate tasks", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listAssignedTasks() throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Listing assigned tasks");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			return engine.listAssignedTasks(caller);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while listing assigned tasks", e);
			throw new RuntimeServiceException("unable to list assigned tasks", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void claimTask(String id) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Claiming task with tid: " + id);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			//TODO Check that the user is allowed to claim the task (or is root)
			engine.claimTask(id, caller);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while claiming task", e);
			throw new RuntimeServiceException("unable to claim task with id: " + id, e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void completeTask(String id, Map<String, Object> variables) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Complete task with tid: " + id);
		try {
			//TODO Check that the user is allowed to complete the task (or is root)
			engine.completeTask(id, variables);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while completing task", e);
			throw new RuntimeServiceException("unable to complete task with id: " + id, e);
		}
	}
		
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public RemoteProcess createRemoteProcess(String key, String toolId, String toolName, String toolKey) throws RuntimeServiceException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Creating new remote process for tool " + toolName + " with key : " + toolKey);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			String id = UUID.randomUUID().toString();
			RemoteProcess remoteProcess = new RemoteProcess();
			remoteProcess.setId(id);
			remoteProcess.setInitier(caller);
			remoteProcess.setKey(key);
			remoteProcess.setToolName(toolName);
			remoteProcess.setToolKey(toolKey);
			remoteProcess.setToolJobId(toolId);
			remoteProcess.setState(State.PENDING);
			remoteProcess.appendLog(new Date() + "  REMOTE PROCESS FOR TOOL " + toolName + " CREATED BY " + caller);
			em.persist(remoteProcess);
			
			registry.register(key, new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, id), caller);
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, RemoteProcess.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, "create"));
			return remoteProcess;
		} catch (RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while creating remote process", e);
			throw new RuntimeServiceException("unable to create remote process ", e);
		}
	}
	

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<RemoteProcess> listRemoteProcesses(State state) throws RuntimeServiceException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Listing " + ((state != null)?state:"all") + " remote processes");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			TypedQuery<RemoteProcess> query;
			if ( state != null ) {
				query = em.createNamedQuery("findRemoteProcessByIniterAndState", RemoteProcess.class).setParameter("state", state).setParameter("initier", caller);
			} else {
				query = em.createNamedQuery("findRemoteProcessByInitier", RemoteProcess.class).setParameter("initier", caller);
			}
			
			List<RemoteProcess> remoteProcesses = query.getResultList();
			List<RemoteProcess> rRemoteProcesses = new ArrayList<RemoteProcess>();
			for (RemoteProcess remoteProcess : remoteProcesses) {
				try {
					String ikey = registry.lookup(remoteProcess.getObjectIdentifier());
					remoteProcess.setKey(ikey);
					rRemoteProcesses.add(remoteProcess);
				} catch ( IdentifierNotRegisteredException e ) {
					LOGGER.log(Level.WARNING, "unregistered remote process found in storage for id: " + remoteProcess.getId());
				}
			}
			return rRemoteProcesses;
			
		} catch ( RegistryServiceException e ) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while listing remote processes", e);
			throw new RuntimeServiceException("unable to list remote processes", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public RemoteProcess readRemoteProcess(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "Reading remote process with key: " + key);
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, RemoteProcess.OBJECT_TYPE);
			RemoteProcess instance = em.find(RemoteProcess.class, identifier.getId());
			if ( instance == null )  {
				throw new RuntimeServiceException("unable to find a remote process with id: " + identifier.getId());
			}
			instance.setKey(key);

			return instance;
		} catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred while reading remote process", e);
			throw new RuntimeServiceException("unable to read remote process", e);
		}
	}
	
//	@Override
//	@TransactionAttribute(TransactionAttributeType.REQUIRED)
//	public void updateRemoteProcessState(String key, State state) throws RuntimeServiceException {
//		LOGGER.log(Level.INFO, "Updating state of remote process with key: " + key);
//		try {
//			OrtolangObjectIdentifier identifier = registry.lookup(key);
//			checkObjectType(identifier, RemoteProcess.OBJECT_TYPE);
//			RemoteProcess remoteProcess = em.find(RemoteProcess.class, identifier.getId());
//			if ( remoteProcess == null )  {
//				throw new RuntimeServiceException("unable to find a remote process with id: " + identifier.getId());
//			}
//			
//			remoteProcess.setState(state);
//			remoteProcess.appendLog("## REMOTE PROCESS STATE CHANGED TO " + state + " ON " + new Date());
//			em.merge(remoteProcess);
//			
//			registry.update(key);
//			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("state", state);
//			notification.throwEvent(key, RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, "update-state"), argumentsBuilder.build());
//		
//		} catch (Exception e) {
//			ctx.setRollbackOnly();
//			LOGGER.log(Level.SEVERE, "unexpected error occurred while updating remote process state", e);
//			throw new RuntimeServiceException("unable to update remote process state", e);
//		}
//	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateRemoteProcessState(String key, State state, Long start, Long stop) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Updating state of remote process with key: " + key);
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, RemoteProcess.OBJECT_TYPE);
			RemoteProcess remoteProcess = em.find(RemoteProcess.class, identifier.getId());
			if ( remoteProcess == null )  {
				throw new RuntimeServiceException("unable to find a remote process with id: " + identifier.getId());
			}
			
			remoteProcess.setState(state);
			remoteProcess.appendLog(new Date() + "  REMOTE PROCESS STATE CHANGED TO " + state);
			if(start !=null && start != 0){
				remoteProcess.setStart(start);
			}
			if(stop !=null && stop != 0){
				remoteProcess.setStop(stop);
			}
			em.merge(remoteProcess);
			
			registry.update(key);
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("state", state.name());
			notification.throwEvent(key, RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, "change-state"), argumentsBuilder.build());
		
		} catch (Exception e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while updating remote process state", e);
			throw new RuntimeServiceException("unable to update remote process state", e);
		}
	}
	

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void appendRemoteProcessLog(String key, String log) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Appending log to remote process with key: " + key);
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, RemoteProcess.OBJECT_TYPE);
			RemoteProcess remoteProcess = em.find(RemoteProcess.class, identifier.getId());
			if ( remoteProcess == null )  {
				throw new RuntimeServiceException("unable to find a remote process with id: " + identifier.getId());
			}
			remoteProcess.appendLog(log);
			em.merge(remoteProcess);
			
			registry.update(key);
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("message", log);
			notification.throwEvent(key, RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, "log-info"), argumentsBuilder.build());
		} catch (Exception e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while appending log to process", e);
			throw new RuntimeServiceException("unable to append process log", e);
		}
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateRemoteProcessActivity(String key, String name) throws RuntimeServiceException {
		LOGGER.log(Level.INFO, "Updating activity of remote process with key: " + key);
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, RemoteProcess.OBJECT_TYPE);
			RemoteProcess remoteProcess = em.find(RemoteProcess.class, identifier.getId());
			if ( remoteProcess == null )  {
				throw new RuntimeServiceException("unable to find a remote process with id: " + identifier.getId());
			}
			remoteProcess.setActivity(name);
			em.merge(remoteProcess);
			
			registry.update(key);
			ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("activity", name);
			notification.throwEvent(key, RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, OrtolangEvent.buildEventType(RuntimeService.SERVICE_NAME, RemoteProcess.OBJECT_TYPE, "change-activity"), argumentsBuilder.build());
		} catch (Exception e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unexpected error occurred while updating remote process activity", e);
			throw new RuntimeServiceException("unable to update remote process activity", e);
		}
	}
	
	@Override
	public String getServiceName() {
		return RuntimeService.SERVICE_NAME;
	}
	
	@Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
	public String[] getObjectTypeList() {
		return OBJECT_TYPE_LIST;
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
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(RuntimeService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Process.OBJECT_TYPE)) {
				return readProcess(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (RuntimeServiceException | RegistryServiceException | KeyNotFoundException | AccessDeniedException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

    // @TODO implement getSize
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
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
