package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;
import org.jboss.logmanager.Level;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class HandleSnapshotTask extends RuntimeEngineTask {

	public static final String NAME = "Generate Handles";
	
	private static final Logger LOGGER = Logger.getLogger(HandleSnapshotTask.class.getName());

	public HandleSnapshotTask() {
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
		
		try {
			if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
				LOGGER.log(Level.FINE, "starting new user transaction.");
				getUserTransaction().begin();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
		}
		
		Map<String, String> map;
		try {
			LOGGER.log(Level.INFO, "listing snapshot content...");
			//TODO change that with a method able to directly provide handles list, including handles recovered from metadata
			//TODO inject prefix alias and version in handle name and generate consultation url for this handle (if root, special case)
			//TODO find a handle metadata in order to record custom handles
			map = getCoreService().listWorkspaceContent(wskey, snapshot);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to list snapshot content", e);
		}
		LOGGER.log(Level.INFO, "snapshot content map built containing " + map.size() + " entries");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "SnapshotContent map built, containing " + map.size() + " entries"));
		
		boolean needcommit;
		long tscommit = System.currentTimeMillis();
		StringBuilder report = new StringBuilder();
		LOGGER.log(Level.INFO, "starting handles generation");
		for (Entry<String, String> entry : map.entrySet()) {
			needcommit = false;
			try {
				LOGGER.log(Level.INFO, "workspace content entry : " + entry.getKey());
				report.append("handle [").append(entry.getKey()).append("] generated\r\n");
				//getHandleStore().recordHandle(entry.getKey(), entry.getValue(), url);
			} catch (Exception e) {
				LOGGER.log(Level.INFO, "handle [" + entry.getKey() + "] failed to generate: " + e.getMessage());
				report.append("handle [").append(entry.getKey()).append("] failed to generate: ").append(e.getMessage()).append("\r\n");
			}
			if ( System.currentTimeMillis() - tscommit > 30000 ) {
				LOGGER.log(Level.FINE, "current transaction exceed 30sec, need commit.");
				needcommit = true;
			}
			try {
				if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
					LOGGER.log(Level.FINE, "commiting active user transaction.");
					getUserTransaction().commit();
					tscommit = System.currentTimeMillis();
					getUserTransaction().begin();
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
			}
		}
		try {
			LOGGER.log(Level.FINE, "commiting active user transaction.");
			getUserTransaction().commit();
			getUserTransaction().begin();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
		}

		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handle generation done report : \r\n" + report.toString()));
	}
	
	@Override
	public String getTaskName() {
		return NAME;
	}
	
}
