package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class TagSnapshotTask extends RuntimeEngineTask {

	public static final String NAME = "Tag Workspace";
	
	private static final Logger LOGGER = Logger.getLogger(TagSnapshotTask.class.getName());

	public TagSnapshotTask() {
	}

	@Override
	@SuppressWarnings("rawtypes")
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if (!execution.hasVariable(SNAPSHOT_NAME_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + SNAPSHOT_NAME_PARAM_NAME + " is not set");
		}
		String snapshot = execution.getVariable(SNAPSHOT_NAME_PARAM_NAME, String.class);
		String tag = "v" + snapshot;
		if (execution.hasVariable(WORKSPACE_TAG_PARAM_NAME)) {
			tag = execution.getVariable(WORKSPACE_TAG_PARAM_NAME, String.class);
		} else if (execution.hasVariable(SNAPSHOTS_TAGS_PARAM_NAME)) {
            Map tags = execution.getVariable(SNAPSHOTS_TAGS_PARAM_NAME, Map.class);
            if ( tags.containsKey(snapshot) ) {
                tag = (String) tags.get(snapshot);
            }
        }  
		
		try {
			if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
			    LOGGER.log(Level.FINE, "START User Transaction");
				getUserTransaction().begin();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
		}
		
		try {
			LOGGER.log(Level.FINE, "tagging workspace snapshot...");
			getCoreService().tagWorkspace(wskey, tag, snapshot);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to tag workspace snapshot", e);
		}

		try {
			LOGGER.log(Level.FINE, "commiting active user transaction.");
			getUserTransaction().commit();
			getUserTransaction().begin();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
		}

		LOGGER.log(Level.FINE, "workspace snapshot [" + snapshot + "] tagged as [" + tag + "]");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace Snapshot [" + snapshot + "] tagged as [" + tag + "]"));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}
