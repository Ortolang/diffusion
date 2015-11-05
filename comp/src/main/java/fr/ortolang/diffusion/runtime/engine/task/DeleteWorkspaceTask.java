package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.SystemException;

import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class DeleteWorkspaceTask extends RuntimeEngineTask {
	
	private static final Logger LOGGER = Logger.getLogger(DeleteWorkspaceTask.class.getName());
	public static final String NAME = "Delete Workspace";
	
	public DeleteWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		String wskey;
		if (execution.hasVariable(WORKSPACE_ALIAS_PARAM_NAME)) {
			String wsalias = execution.getVariable(WORKSPACE_ALIAS_PARAM_NAME, String.class);
			try {
				wskey = getCoreService().resolveWorkspaceAlias(wsalias);
			} catch (CoreServiceException | AccessDeniedException | AliasNotFoundException e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occurred while resolving workspace alias: " + wsalias + " " + e.getMessage()));
				throw new RuntimeEngineTaskException("unexpected error occurred while resolving workspace alias: " + wsalias, e);
			}
		} else {
	        wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		}
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_NAME_PARAM_NAME, wskey);
		}
		
		try {
			try {
				getUserTransaction().setTransactionTimeout(2000);
				LOGGER.log(Level.FINE, "Reading workspace");
		        Workspace workspace = getCoreService().readWorkspace(wskey);
				if ( workspace.getType().equals(WorkspaceType.SYSTEM.toString())) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace is a SYSTEM workspace and it is forbidden to delete it"));
					throw new RuntimeEngineTaskException("deleting a system workspace is forbidden");
				}
				LOGGER.log(Level.FINE, "Listing workspace keys");
				Set<String> keys = getCoreService().systemListWorkspaceKeys(wskey);
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace content retrieved"));
				StringBuilder trace = new StringBuilder();
				getCoreService().deleteWorkspace(wskey);
                for ( String key : keys ) {
					LOGGER.log(Level.FINE, "Deleting content key: " + key);
					getRegistryService().delete(key, true);
					getIndexingService().remove(key);
					trace.append("key [").append(key).append("] deleted and removed from index");
				}
				trace.append("workspace with key [").append(wskey).append("] deleted");
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace deleted"));
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), trace.toString(), null));
			} catch (KeyNotFoundException | IndexingServiceException | AccessDeniedException | CoreServiceException | RegistryServiceException | KeyLockedException | WorkspaceReadOnlyException e) {
				getUserTransaction().rollback();
				throw new RuntimeEngineTaskException("unexpected error during delete workspace task", e);
			} 
		} catch (SystemException | SecurityException | IllegalStateException  | EJBTransactionRolledbackException e) {
		    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
			throw new RuntimeEngineTaskException("unexpected error occurred", e);
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}