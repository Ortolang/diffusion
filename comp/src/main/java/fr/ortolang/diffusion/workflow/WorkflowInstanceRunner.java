package fr.ortolang.diffusion.workflow;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.RuntimeService;

import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;

public class WorkflowInstanceRunner implements Runnable {

	private Logger logger = Logger.getLogger(WorkflowInstanceRunner.class.getName());

	private RuntimeService runtime;
	private WorkflowInstance instance;

	public WorkflowInstanceRunner(RuntimeService runtime, WorkflowInstance instance) {
		this.runtime = runtime;
		this.instance = instance;
	}

	public RuntimeService getRuntime() {
		return runtime;
	}

	public void setRuntime(RuntimeService runtime) {
		this.runtime = runtime;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public WorkflowInstance getInstance() {
		return instance;
	}

	public void setInstance(WorkflowInstance instance) {
		this.instance = instance;
	}

	@Override
	public void run() {
		logger.log(Level.FINE, "Starting workflow instance runner for workflow instance id: " + instance.getId());
		runtime.startProcessInstanceById(instance.getDefinitionId(), instance.getId(), instance.getParams());
		logger.log(Level.FINE, "Workflow instance runner stopped for workflow instance id: " + instance.getId());
	}

}
