package fr.ortolang.diffusion.runtime.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;

public class RuntimeEngineListener {

	private Logger logger = Logger.getLogger(RuntimeEngineListener.class.getName());

	private RuntimeService runtime;

	public RuntimeService getRuntimeService() throws RuntimeEngineException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch (Exception e) {
			throw new RuntimeEngineException(e);
		}
	}

	public void onEvent(RuntimeEngineEvent event) {
		try {
			logger.log(Level.FINE, "RuntimeEngineEvent type: " + event.getType());
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig
					.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_START)) {
					getRuntimeService().updateProcessState(event.getPid(), State.RUNNING);
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ABORT)) {
					getRuntimeService().updateProcessState(event.getPid(), State.ABORTED);
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_COMPLETE)) {
					getRuntimeService().updateProcessState(event.getPid(), State.COMPLETED);
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_LOG)) {
					getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_STARTED)) {
					getRuntimeService().updateProcessActivity(event.getPid(), event.getActivityName());
					getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_ERROR)) {
					getRuntimeService().updateProcessActivity(event.getPid(), "");
					getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_PROGRESS)) {
					// TODO Increase RuntimeService Process model to include activity progression field
					getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
				}
				if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_COMPLETED)) {
					getRuntimeService().updateProcessActivity(event.getPid(), "");
					getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
				}
			} catch (RuntimeServiceException | RuntimeEngineException e) {
				logger.log(Level.SEVERE, "unexpected error while trying to treat runtime  event", e);
			} finally {
				loginContext.logout();
			}
		} catch (LoginException e) {
			logger.log(Level.SEVERE, "RuntimeEventListener login error", e);
		}
	}

}