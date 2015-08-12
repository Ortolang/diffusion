package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class SubmitSnapshotTask extends RuntimeEngineTask {

	public static final String NAME = "Submit Workspace For Review";
	
	private static final Logger LOGGER = Logger.getLogger(SubmitSnapshotTask.class.getName());

	public SubmitSnapshotTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if (!execution.hasVariable(SNAPSHOT_NAME_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + SNAPSHOT_NAME_PARAM_NAME + " is not set");
		}
		String snapshot = execution.getVariable(SNAPSHOT_NAME_PARAM_NAME, String.class);
		
		Set<String> keys;
		try {
			LOGGER.log(Level.FINE, "building review list...");
			keys = getCoreService().buildWorkspaceReviewList(wskey, snapshot);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to built the review list", e);
		}
		LOGGER.log(Level.FINE, "review list containing " + keys.size() + " keys");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "ReviewList built, containing " + keys.size() + " elements"));

		StringBuilder report = new StringBuilder();
		LOGGER.log(Level.FINE, "starting review");
		for (String key : keys) {
			try {
				getPublicationService().review(key);
				report.append("key [").append(key).append("] locked for review\r\n");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "key [" + key + "] failed to lock for review: " + e.getMessage());
				report.append("key [").append(key).append("] failed to lock for review: ").append(e.getMessage()).append("\r\n");
			}
		}
		
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "lock for review done: \r\n" + report.toString()));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}