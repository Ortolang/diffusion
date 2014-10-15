package fr.ortolang.diffusion.runtime.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.TaskService;

import fr.ortolang.diffusion.runtime.entity.ProcessTask;

public class ActivitiTaskRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiTaskRunner.class.getName());

	private TaskService service;
	private ProcessTask task;
	private Map<String, Object> variables;

	public ActivitiTaskRunner(TaskService service, ProcessTask task, Map<String, Object> variables) {
		this.service = service;
		this.task = task;
		this.variables = variables;
	}

	@Override
	public void run() {
		logger.log(Level.FINE, "Starting task runner for taskid: " + task.getId());
		service.complete(task.getId(), variables);
		logger.log(Level.FINE, "Task runner stopped for taskid: " + task.getId());
	}

}