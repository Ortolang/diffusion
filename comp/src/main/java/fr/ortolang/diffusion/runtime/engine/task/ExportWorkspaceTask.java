package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class ExportWorkspaceTask extends RuntimeEngineTask {
	
	public static final String NAME = "Export Workspace";
	
	private static final Logger logger = Logger.getLogger(ExportWorkspaceTask.class.getName());

	public ExportWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		logger.log(Level.INFO, "Starting Export Workspace Task");
		
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}