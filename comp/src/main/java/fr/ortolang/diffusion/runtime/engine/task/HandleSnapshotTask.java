package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Set;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;
import org.jboss.logmanager.Level;

import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
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
		
		Set<OrtolangObjectPid> pids;
		try {
			LOGGER.log(Level.FINE, "generating workspace handles...");
			Workspace workspace = getCoreService().readWorkspace(wskey);
			TagElement te = workspace.findTagBySnapshot(snapshot);
			pids = getCoreService().buildWorkspacePidList(wskey, te.getName());
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to generate handle list", e);
		}
		LOGGER.log(Level.FINE, "pids list containing " + pids.size() + " entries");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "pids list built, containing " + pids.size() + " entries"));
		
		boolean needcommit;
		long tscommit = System.currentTimeMillis();
		StringBuilder report = new StringBuilder();
		LOGGER.log(Level.FINE, "starting pids creation");
		for (OrtolangObjectPid pid : pids) {
			needcommit = false;
			try {
				getHandleStore().recordHandle(pid.getName(), pid.getKey(), pid.getTarget());
				LOGGER.log(Level.FINE, pid + " created");
				report.append(pid).append(" created\r\n");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "handle [" + pid.getName() + "] failed to generate: " + e.getMessage());
				report.append(pid).append(" creation FAILED: ").append(e.getMessage()).append("\r\n");
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
