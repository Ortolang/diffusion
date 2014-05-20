package fr.ortolang.diffusion.workflow.process;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.workflow.WorkflowServiceLocal;

public abstract class ProcessExecutionTask implements Runnable {

	private Logger logger = Logger.getLogger(ProcessExecutionTask.class.getName());

	private String processKey;
	private String caller;
	private Map<String, String> params;
	private WorkflowServiceLocal workflow;

	public ProcessExecutionTask() {
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(String key) {
		this.processKey = key;
	}

	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	
	public WorkflowServiceLocal getWorkflowService() throws Exception {
		if ( workflow == null ) {
			workflow = (WorkflowServiceLocal) OrtolangServiceLocator.findLocalService("workflow");
		}
		return workflow;
	}
	
	public void log(String entry) throws Exception {
		getWorkflowService().addProcessExecutionLog(getProcessKey(), entry);
	}

	@Override
	public void run() {
		try {
			getWorkflowService().startProcessExecution(getProcessKey());
			try {
				process();
			} catch (Exception e) {
				logger.log(Level.WARNING, "An error occured during process execution", e);
				this.log("an error occured during process execution: " + e.getMessage());
				getWorkflowService().stopProcessExecution(getProcessKey(), -1);
				return;
			}
			getWorkflowService().stopProcessExecution(getProcessKey(), 0);
		} catch (Exception e) {
			logger.log(Level.WARNING, "error during process execution task",e);
		}
	}

	public abstract void process() throws Exception;

}
