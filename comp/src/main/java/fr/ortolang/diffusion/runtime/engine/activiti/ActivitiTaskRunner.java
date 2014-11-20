package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;

public class ActivitiTaskRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiTaskRunner.class.getName());

	private ActivitiEngineBean engine;
	private String task;
	private Map<String, Object> variables;

	public ActivitiTaskRunner(ActivitiEngineBean engine, String task, Map<String, Object> variables) {
		this.engine = engine;
		this.task = task;
		this.variables = variables;
	}

	@Override
	public void run() {
		logger.log(Level.INFO, "Starting task runner for task: " + task);
		try {
			engine.getActivitiTaskService().complete(task, variables);
		} catch ( ActivitiObjectNotFoundException e ) {
			logger.log(Level.WARNING, "Error during task execution", e);
		}
		logger.log(Level.INFO, "Task runner stopped for task: " + task);
	}

}