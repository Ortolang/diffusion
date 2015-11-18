package fr.ortolang.diffusion.runtime.engine.activiti;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngine;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineListener;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.ProcessType;

@Startup
@Local(RuntimeEngine.class)
@Singleton(name = RuntimeEngine.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class ActivitiEngineBean implements RuntimeEngine, ActivitiEventListener {

	private static final Logger LOGGER = Logger.getLogger(ActivitiEngineBean.class.getName());

	@Resource
	private ManagedScheduledExecutorService scheduledExecutor;
	@Resource
	private ManagedExecutorService executor;
	@Resource
	private ContextService contextService;
	@PersistenceUnit(unitName = "ortolangPU")
	private EntityManagerFactory emf;

	private ProcessEngine engine;
	private RuntimeEngineListener listener;

	public ActivitiEngineBean() {
	}

	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initilizing EngineServiceBean");
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
			engine.getRuntimeService().addEventListener(this);
			listener = new RuntimeEngineListener();
			LOGGER.log(Level.INFO, "Activiti Engine created: " + engine.getName());
		}
		LOGGER.log(Level.INFO, "EngineServiceBean initialized");
	}

	@PreDestroy
	public void dispose() {
		LOGGER.log(Level.INFO, "Stopping EngineServiceBean");
		if (engine != null) {
			engine.close();
		}
		LOGGER.log(Level.INFO, "EngineServiceBean stopped");
	}

	protected RuntimeService getActivitiRuntimeService() {
		return engine.getRuntimeService();
	}

	protected TaskService getActivitiTaskService() {
		return engine.getTaskService();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deployDefinitions(String[] resources) {
		LOGGER.log(Level.INFO, "Deploying all process definitions");
		for (String resource : resources) {
		    LOGGER.log(Level.INFO, "Creating deployment builder for process definition resource: " + resource);
            DeploymentBuilder deployment = engine.getRepositoryService().createDeployment();
    		deployment.enableDuplicateFiltering();
    		deployment.name("Deployment of resource: " + resource);
    		deployment.addClasspathResource(resource);
			Deployment deploy = deployment.deploy();
			LOGGER.log(Level.INFO, "Process definitions deployed on " + deploy.getDeploymentTime());
		}
		LOGGER.log(Level.INFO, "All process definitions deployed");
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ProcessType> listProcessTypes(boolean latest) throws RuntimeEngineException {
	    List<ProcessDefinition> apdefs;
	    if ( latest ) {
	        apdefs = engine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list();
	    } else {
	        apdefs = engine.getRepositoryService().createProcessDefinitionQuery().list();
	    }
		List<ProcessType> defs = new ArrayList<ProcessType>();
		for (ProcessDefinition apdef : apdefs) {
			String form = engine.getFormService().getStartFormKey(apdef.getId());
			defs.add(toProcessType(apdef, form));
		}
		return defs;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessType getProcessTypeById(String id) throws RuntimeEngineException {
		ProcessDefinition apdef = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(id).singleResult();
		String form = engine.getFormService().getStartFormKey(apdef.getId());
		return toProcessType(apdef, form);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ProcessType getProcessTypeByKey(String key) throws RuntimeEngineException {
		ProcessDefinition apdef = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();
		String form = engine.getFormService().getStartFormKey(apdef.getId());
		return toProcessType(apdef, form);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void startProcess(String type, String key, Map<String, Object> variables) throws RuntimeEngineException {
		ActivitiProcessRunner runnable = new ActivitiProcessRunner(this, type, key, variables);
		Runnable ctxRunnable = contextService.createContextualProxy(runnable, Runnable.class);
		scheduledExecutor.schedule(ctxRunnable, 3, TimeUnit.SECONDS);
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteProcess(String key) throws RuntimeEngineException {
	    try {
            ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(key).singleResult();
            engine.getRuntimeService().deleteProcessInstance(instance.getId(), "Ortolang process deleted also");
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while deleting process instance", e);
        }
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
    public List<Process> findProcess(Map<String, Object> variables) throws RuntimeEngineException {
        try {
            ProcessInstanceQuery query = engine.getRuntimeService().createProcessInstanceQuery();
            for ( Entry<String, Object> variableEntry : variables.entrySet() ) {
                query.variableValueEquals(variableEntry.getKey(), variableEntry.getValue());
            }
            List<Process> result = new ArrayList<Process> ();
            List<ProcessInstance> instances = query.list();
            for ( ProcessInstance instance : instances ) {
                result.add(toProcess(instance));
            }
            return result;
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while trying to find process instances", e);
        }
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public HumanTask getTask(String id) throws RuntimeEngineException {
		try {
			Task task = engine.getTaskService().createTaskQuery().taskId(id).singleResult();
			String form = engine.getFormService().getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
			return toHumanTask(task, form);
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while getting task", e);
		}
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<HumanTask> listAllTasks() throws RuntimeEngineException {
        try {
            List<HumanTask> tasks = new ArrayList<HumanTask>();

            List<Task> utasks = engine.getTaskService().createTaskQuery().list();
            for (Task task : utasks) {
                String form = engine.getFormService().getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
                tasks.add(toHumanTask(task, form));
            }
            
            return tasks;
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while listing all tasks", e);
        }
    }
	
	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isCandidate(String taskid, String user, List<String> groups) throws RuntimeEngineException {
        try {
            Task t1 = engine.getTaskService().createTaskQuery().taskId(taskid).taskCandidateUser(user).singleResult();
            Task t2 = engine.getTaskService().createTaskQuery().taskId(taskid).taskCandidateGroupIn(groups).singleResult();
            return t1 != null || t2 != null;
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while checking if is candidate", e);
        }
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listCandidateTasks(String user, List<String> groups) throws RuntimeEngineException {
		try {
			List<HumanTask> ctasks = new ArrayList<HumanTask>();

			List<Task> cutasks = engine.getTaskService().createTaskQuery().taskCandidateUser(user).list();
			for (Task task : cutasks) {
				String form = engine.getFormService().getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
				ctasks.add(toHumanTask(task, form));
			}
			if ( groups != null && groups.size() > 0 ) {
				List<Task> cgtasks = engine.getTaskService().createTaskQuery().taskCandidateGroupIn(groups).list();
				for (Task task : cgtasks) {
					String form = engine.getFormService().getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
					ctasks.add(toHumanTask(task, form));
				}
			}

			return ctasks;
		} catch (ActivitiException e) {
			throw new RuntimeEngineException("unexpected error while listing candidate tasks", e);
		}
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isAssigned(String taskid, String user) throws RuntimeEngineException {
        try {
            Task cutask = engine.getTaskService().createTaskQuery().taskId(taskid).taskAssignee(user).singleResult();
            return cutask != null;
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while checking if is assigned", e);
        }
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<HumanTask> listAssignedTasks(String user) throws RuntimeEngineException {
		try {
			List<HumanTask> atasks = new ArrayList<HumanTask>();
			List<Task> autasks = engine.getTaskService().createTaskQuery().taskAssignee(user).list();
			for (Task task : autasks) {
				String form = engine.getFormService().getTaskFormKey(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
				atasks.add(toHumanTask(task, form));
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
    public void unclaimTask(String id) throws RuntimeEngineException {
        try {
            engine.getTaskService().unclaim(id);
        } catch (ActivitiException e) {
            throw new RuntimeEngineException("unexpected error while unclaiming process task", e);
        }
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void completeTask(String id, Map<String, Object> variables) throws RuntimeEngineException {
		ActivitiTaskRunner runnable = new ActivitiTaskRunner(this, id, variables);
		Runnable ctxRunnable = contextService.createContextualProxy(runnable, Runnable.class);
		scheduledExecutor.schedule(ctxRunnable, 3, TimeUnit.SECONDS);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void notify(RuntimeEngineEvent event) throws RuntimeEngineException {
		listener.onEvent(event);
	}

	@Override
	@SuppressWarnings("incomplete-switch")
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void onEvent(ActivitiEvent event) {
		try {
            String pid;
            TaskEntity task;
            switch (event.getType()) {
                case PROCESS_COMPLETED:
                    LOGGER.log(Level.INFO, "Activiti process completed event received");
                    pid = ((ExecutionEntity)((ActivitiEntityEvent)event).getEntity()).getBusinessKey();
                    notify(RuntimeEngineEvent.createProcessCompleteEvent(pid));
                    break;
                case TASK_CREATED:
                    LOGGER.log(Level.INFO, "Activiti task created event received");
                    task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
                    pid = task.getProcessInstance().getBusinessKey();
                    LOGGER.log(Level.FINEST, "pid of task: " + pid);
                    notify(RuntimeEngineEvent.createTaskCreatedEvent(pid, task.getId(), task.getName(), task.getCandidates()));
                    break;
                case TASK_ASSIGNED:
                    LOGGER.log(Level.INFO, "Activiti task assigned event received");
                    task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
                    pid = task.getProcessInstance().getBusinessKey();
                    LOGGER.log(Level.FINEST, "pid of task: " + pid);
                    notify(RuntimeEngineEvent.createTaskAssignedEvent(pid, task.getId(), task.getName(), task.getAssignee()));
                    break;
                case TASK_COMPLETED:
                    LOGGER.log(Level.INFO, "Activiti task completed event received");
                    task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
                    pid = task.getProcessInstance().getBusinessKey();
                    LOGGER.log(Level.FINEST, "pid of task: " + pid);
                    notify(RuntimeEngineEvent.createTaskCompletedEvent(pid, task.getId(), task.getName(), task.getAssignee()));
                    break;
            }
		} catch ( RuntimeEngineException e ) {
			LOGGER.log(Level.WARNING, "unexpected error during treating activiti event", e);
		}
	}

	@Override
	public boolean isFailOnException() {
		return false;
	}

	private ProcessType toProcessType(ProcessDefinition def, String startform) {
		ProcessType instance = new ProcessType();
		instance.setId(def.getId());
		instance.setName(def.getKey());
		instance.setDescription(def.getDescription());
		instance.setVersion(def.getVersion());
		instance.setSuspended(def.isSuspended());
		instance.setStartForm(startform);
		return instance;
	}

	private Process toProcess(ProcessInstance pins) {
		Process instance = new Process();
		instance.setId(pins.getBusinessKey());
		instance.setName(pins.getName());

		return instance;
	}

	private HumanTask toHumanTask(Task task, String form) {
		HumanTask instance = new HumanTask();
		instance.setId(task.getId());
		instance.setName(task.getName());
		instance.setDescription(task.getDescription());
		instance.setOwner(task.getOwner());
		instance.setAssignee(task.getAssignee());
		instance.setCreationDate(task.getCreateTime());
		instance.setDueDate(task.getDueDate());
		instance.setPriority(task.getPriority());
		instance.setForm(form);
		return instance;
	}

}
