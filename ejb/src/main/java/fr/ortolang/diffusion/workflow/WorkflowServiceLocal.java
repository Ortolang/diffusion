package fr.ortolang.diffusion.workflow;

import fr.ortolang.diffusion.OrtolangService;


public interface WorkflowServiceLocal extends OrtolangService {
	
	public void startProcessExecution(String key) throws WorkflowServiceException;
	
	public void stopProcessExecution(String key, int completed) throws WorkflowServiceException;
	
	public void addProcessExecutionLog(String key, String logentry) throws WorkflowServiceException;

}
