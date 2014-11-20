package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.jboss.logmanager.Level;

import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class SnapshotWorkspaceTask extends RuntimeEngineTask {

	public static final String NAME = "Snapshot Workspace";

	public static final String WORKSPACE_KEY = "workspace-key";
	public static final String SNAPSHOT = "snapshot";
	public static final String ROOT = "root";

	private static final Logger logger = Logger.getLogger(SnapshotWorkspaceTask.class.getName());

	public SnapshotWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY, String.class);
		
		try {
			Workspace workspace = getCoreService().readWorkspace(wskey);
			String snapshotName;
			String rootCollection;
			if (!execution.hasVariable(SNAPSHOT)) {
				if (workspace.hasChanged()) {
					logger.log(Level.INFO, "Snapshot name NOT provided and workspace has changed since last snapshot, generating a new snapshot");
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(),
							"Workspace modified and snapshot name to publish NOT provided, creating a new snapshot"));
					snapshotName = "Version " + workspace.getClock();
					rootCollection = workspace.getHead();
					getCoreService().snapshotWorkspace(wskey, snapshotName);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "New snapshot [" + snapshotName + "] created"));	
				} else {
					logger.log(Level.INFO, "Snapshot name NOT provided and workspace has not changed since last snapshot, loading latest snapshot");
					String head = workspace.getHead();
					rootCollection = getRegistryService().getChildren(head);
					if ( rootCollection == null ) {
						throw new RuntimeEngineTaskException("unable to find an existing snapshot.");
					}
				}
			} else {
				snapshotName = execution.getVariable(SNAPSHOT, String.class);
				SnapshotElement snapshot = workspace.findSnapshotByName(snapshotName);
				if (snapshot == null) {
					throw new RuntimeEngineTaskException("unable to find a snapshot with name " + snapshotName + " in workspace " + wskey);
				}
				rootCollection = snapshot.getKey();
			}
			String publication = getRegistryService().getPublicationStatus(rootCollection);
			if (!publication.equals(OrtolangObjectState.Status.DRAFT.value())) {
				throw new RuntimeEngineTaskException("Snapshot publication status is not " + OrtolangObjectState.Status.DRAFT
						+ ", maybe already published or involved in another publication process");
			}
			logger.log(Level.INFO, "Root collection retreived from snapshot: " + rootCollection);
			execution.setVariable(ROOT, rootCollection);
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Snapshot loaded, going for publishing the associated root collection"));

		} catch (CoreServiceException | KeyNotFoundException | AccessDeniedException | RegistryServiceException e) {
			throw new RuntimeEngineTaskException("unexpected error during snapshot task execution", e);
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	@Override
	public boolean needEngineAuth() {
		return false;
	}

}
