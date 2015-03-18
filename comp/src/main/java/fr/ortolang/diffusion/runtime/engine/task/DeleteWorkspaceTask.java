package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.SystemException;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class DeleteWorkspaceTask extends RuntimeEngineTask {
	
	private static final Logger logger = Logger.getLogger(DeleteWorkspaceTask.class.getName());
	public static final String NAME = "Delete Workspace";
	
	public DeleteWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		logger.log(Level.INFO, "Starting Delete Workspace Task");
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_NAME_PARAM_NAME, wskey);
		}
		
		try {
			try {
				getUserTransaction().setTransactionTimeout(2000);
				logger.log(Level.FINE, "Reading workspace");
				Workspace workspace = getCoreService().readWorkspace(wskey);
				if ( workspace.getType().equals(WorkspaceType.SYSTEM.toString())) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace is a SYSTEM workspace and it si forbidden to delete it"));
					throw new RuntimeEngineTaskException("deleting a system workspace is forbidden");
				}
				logger.log(Level.FINE, "Listing workspace keys");
				Set<String> keys = getCoreService().systemListWorkspaceKeys(wskey);
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace content retreived"));
				for ( String key : keys ) {
					logger.log(Level.FINE, "Deleting content key: " + key);
					getRegistryService().systemDelete(key);
				}
			} catch (KeyNotFoundException | AccessDeniedException | CoreServiceException | RegistryServiceException e) {
				getUserTransaction().rollback();
				throw new RuntimeEngineTaskException("unexpected error during delete workspace task", e);
			} 
		} catch (SystemException | SecurityException | IllegalStateException e) {
			throw new RuntimeEngineTaskException("unable to create transaction", e);
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}