package fr.ortolang.diffusion.workflow.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class SnapshotTask implements JavaDelegate {
	
	private Logger logger = Logger.getLogger(SnapshotTask.class.getName());
	private CoreService core;
	
	public SnapshotTask() {
	}
	
	public CoreService getCoreService() throws OrtolangException {
		if ( core == null ) {
			core = (CoreService) OrtolangServiceLocator.findService("core");
		}
		return core;
	}
	
	@Override
	public void execute(DelegateExecution execution) {
		logger.log(Level.INFO, "Starting execution of SnapshotTask with id " + execution.getId());
		String workspace = (String) execution.getVariable("workspace");
		String name = (String) execution.getVariable("name");
		try {
			getCoreService().snapshotWorkspace(workspace, name);
		} catch ( KeyNotFoundException | AccessDeniedException | CoreServiceException | OrtolangException e) {
			//TODO Handle error case
			logger.log(Level.INFO, "Unexpected error during execution of SnapshotTask with id " + execution.getId(), e);
		}
	}

}
