package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.TaskService;

public class ActivitiTaskRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiTaskRunner.class.getName());

	private TaskService service;
	private String task;
	private Map<String, Object> variables;

	public ActivitiTaskRunner(TaskService service, String task, Map<String, Object> variables) {
		this.service = service;
		this.task = task;
		this.variables = variables;
	}

	@Override
	public void run() {
		logger.log(Level.INFO, "Starting task runner for task: " + task);
		try {
			service.complete(task, variables);
		} catch ( ActivitiObjectNotFoundException e ) {
			logger.log(Level.WARNING, "Error during task execution", e);
		}
		logger.log(Level.INFO, "Task runner stopped for task: " + task);
	}

}