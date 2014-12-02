package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class CreateWorkspaceTask extends RuntimeEngineTask {
	
	public static final String NAME = "Create Workspace";
	
	private static final Logger logger = Logger.getLogger(CreateWorkspaceTask.class.getName());

	public CreateWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_NAME_PARAM_NAME, wskey);
		}
		String wsname = execution.getVariable(WORKSPACE_NAME_PARAM_NAME, String.class);
		if ( !execution.hasVariable(WORKSPACE_TYPE_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_TYPE_PARAM_NAME, "unknown");
		}
		String wstype = execution.getVariable(WORKSPACE_TYPE_PARAM_NAME, String.class);
		
		logger.log(Level.INFO, "Creating workspace");
		try {
			OrtolangObjectIdentifier wsidentifier = getRegistryService().lookup(wskey);
			if ( !wsidentifier.getService().equals(CoreService.SERVICE_NAME) && !wsidentifier.getType().equals(Workspace.OBJECT_TYPE) ) {
				logger.log(Level.SEVERE, "Workspace Key already exists but is not a workspace !!");
				throw new RuntimeEngineTaskException("Workspace Key already exists but is NOT a workspace !!");
			}
		} catch ( KeyNotFoundException e ) {
			try {
				getCoreService().createWorkspace(wskey, wsname, wstype);
			} catch ( Exception e2 ) {
				logger.log(Level.SEVERE, "unable to create workspace", e2);
				throw new RuntimeEngineTaskException("unable to create workspace", e2);
			}
		} catch (RegistryServiceException e) {
			throw new RuntimeEngineTaskException("unable to create workspace", e);
		}
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace created with key: " + wskey));
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