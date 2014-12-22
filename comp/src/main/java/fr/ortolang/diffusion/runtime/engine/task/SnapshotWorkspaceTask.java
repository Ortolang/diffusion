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

	private static final Logger logger = Logger.getLogger(SnapshotWorkspaceTask.class.getName());

	public SnapshotWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		
		try {
			Workspace workspace = getCoreService().readWorkspace(wskey);
			String snapshotName;
			String rootCollection;
			if (!execution.hasVariable(SNAPSHOT_NAME_PARAM_NAME)) {
				if (workspace.hasChanged()) {
					if ( execution.hasVariable(BAG_VERSION_PARAM_NAME) ) {
						logger.log(Level.INFO, "Bag version variable present, parsing snapshot name");
						String version = snapshotName = execution.getVariable(BAG_VERSION_PARAM_NAME, String.class);
						snapshotName = version.substring(version.lastIndexOf("/")+1);
					} else {
						logger.log(Level.INFO, "Snapshot name NOT provided and workspace has changed since last snapshot, generating a new snapshot");
						snapshotName = "Version " + workspace.getClock();
					}
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Creating a new snapshot with name: " + snapshotName));
					rootCollection = workspace.getHead();
					getCoreService().snapshotWorkspace(wskey, snapshotName);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "New snapshot [" + snapshotName + "] created"));	
				} else {
					logger.log(Level.INFO, "Snapshot name NOT provided and workspace has not changed since last snapshot, loading latest snapshot");
					String head = workspace.getHead();
					rootCollection = getRegistryService().getParent(head);
					if ( rootCollection == null ) {
						throw new RuntimeEngineTaskException("unable to find an existing snapshot.");
					}
				}
			} else {
				snapshotName = execution.getVariable(SNAPSHOT_NAME_PARAM_NAME, String.class);
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
			execution.setVariable(ROOT_COLLECTION_PARAM_NAME, rootCollection);
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Snapshot loaded, setting the root collection as variable"));

		} catch (CoreServiceException | KeyNotFoundException | AccessDeniedException | RegistryServiceException e) {
			throw new RuntimeEngineTaskException("unexpected error during snapshot task execution", e);
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}
