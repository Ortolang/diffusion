package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.ActivitiSignalEvent;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;

public class ActivitiEngineListener implements ActivitiEventListener {

	private Logger logger = Logger.getLogger(ActivitiEngineListener.class.getName());
	private RuntimeService runtime;
	private UserTransaction userTx;

	public ActivitiEngineListener() {
	}

	private RuntimeService getRuntimeService() throws OrtolangException {
		if (runtime == null) {
			runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
		}
		return runtime;
	}
	
	public UserTransaction getUserTransaction() throws RuntimeEngineTaskException {
		try {
			if (userTx == null) {
				userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
			}
			return userTx;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	@Override
	public void onEvent(ActivitiEvent event) {
		logger.log(Level.INFO, "Event received: " + event.getType());
		if (event.getType().equals(ActivitiEventType.PROCESS_COMPLETED)) {
			String pid = ((ExecutionEntity)((ActivitiEntityEvent)event).getEntity()).getBusinessKey();
			updateProcessState(pid, State.COMPLETED);
		}
		if (event.getType().equals(ActivitiEventType.ACTIVITY_SIGNALED)) {
			ActivitiSignalEvent aevent = ((ActivitiSignalEvent)event);
			if ( aevent.getActivityId().equals("ORTOLANG") ) {
				if ( aevent.getSignalName().equals("PROCESS_STATE_UPDATE") ) {
					updateProcessState((String)aevent.getProcessInstanceId(), (State)aevent.getSignalData());
				}
				if ( aevent.getSignalName().equals("PROCESS_ACTIVITY_UPDATE") ) {
					updateProcessActiviti((String)aevent.getProcessInstanceId(), (String)aevent.getSignalData());
				}
				if ( aevent.getSignalName().equals("PROCESS_APPEND_LOG") ) {
					appendProcessLog((String)aevent.getProcessInstanceId(), (String)aevent.getSignalData());
				}
			} 
		}
		if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {
			TaskEntity task = (TaskEntity)((ActivitiEntityEvent)event).getEntity();
			String key = task.getExecution().getBusinessKey();
			updateProcessActiviti(key, task.getName());
		}
	}

	@Override
	public boolean isFailOnException() {
		return false;
	}
	
	private void updateProcessState(String pid, State state) {
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				getRuntimeService().updateProcessState(pid, state);
			} catch (RuntimeServiceException | OrtolangException | SecurityException | IllegalStateException e) {
				logger.log(Level.SEVERE, "unable to update process state", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "unable to update process state", e);
		}
	}
	
	private void updateProcessActiviti(String pid, String activity) {
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				getRuntimeService().updateProcessActivity(pid, activity);
			} catch (RuntimeServiceException | OrtolangException | SecurityException | IllegalStateException e) {
				logger.log(Level.SEVERE, "unable to update process activity", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "unable to update process activity", e);
		}
	}
	
	private void appendProcessLog(String pid, String log) {
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				getRuntimeService().appendProcessLog(pid, log);
			} catch (RuntimeServiceException | OrtolangException | SecurityException | IllegalStateException e) {
				logger.log(Level.SEVERE, "unable to append process log", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "unable to append process log", e);
		}
	}

}
