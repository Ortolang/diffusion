package fr.ortolang.diffusion.runtime.activiti;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;

public class ActivitiEngineListener implements ActivitiEventListener {

	private Logger logger = Logger.getLogger(ActivitiEngineListener.class.getName());
	private RuntimeService runtime;

	public ActivitiEngineListener() {
	}

	private RuntimeService getRuntimeService() throws OrtolangException {
		if (runtime == null) {
			runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
		}
		return runtime;
	}

	public void onProcessStart(String pid) {
		logger.log(Level.INFO, "Process Start received: " + pid);
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("activiti", "activiti54");
			loginContext.login();
			try {
				getRuntimeService().updateProcessState(pid, State.RUNNING);
			} catch (RuntimeServiceException | OrtolangException e) {
				logger.log(Level.SEVERE, "unable to update process state", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "unable to update process state", e);
		}
	}

	public void onProcessError(String pid, String cause) {
		logger.log(Level.INFO, "Process Error received: " + pid);
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("activiti", "activiti54");
			loginContext.login();
			try {
				getRuntimeService().updateProcessState(pid, State.ABORTED);
				getRuntimeService().appendProcessLog(pid, "error occured during process: " + cause);
			} catch (RuntimeServiceException | OrtolangException e) {
				logger.log(Level.SEVERE, "unable to update process state", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "unable to update process state", e);
		}
	}

	@Override
	public void onEvent(ActivitiEvent event) {
		if (event.getType().equals(ActivitiEventType.PROCESS_COMPLETED)) {
			try {
				LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("activiti", "activiti54");
				loginContext.login();
				try {
					String pid = ((ExecutionEntity)((ActivitiEntityEvent)event).getEntity()).getBusinessKey();
					getRuntimeService().updateProcessState(pid, State.COMPLETED);
				} catch (RuntimeServiceException | OrtolangException e) {
					logger.log(Level.SEVERE, "unable to update process state", e);
				} finally {
					loginContext.logout();
				}
			} catch (LoginException e) {
				logger.log(Level.SEVERE, "unable to update process state", e);
			}
		}
		if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {
			TaskEntity task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
			String key = task.getExecution().getBusinessKey();
			logger.log(Level.INFO, "task created for process with key : " + key);
		}
		if (event.getType().equals(ActivitiEventType.TASK_ASSIGNED)) {
			TaskEntity task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
			String key = task.getExecution().getBusinessKey();
			logger.log(Level.INFO, "task assigned for process with key : " + key);
		}
		if (event.getType().equals(ActivitiEventType.TASK_COMPLETED)) {
			TaskEntity task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
			String key = task.getExecution().getBusinessKey();
			logger.log(Level.INFO, "task completed for process with key : " + key);
		}
	}

	@Override
	public boolean isFailOnException() {
		return false;
	}

}
