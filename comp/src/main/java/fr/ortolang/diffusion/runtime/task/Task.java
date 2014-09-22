package fr.ortolang.diffusion.runtime.task;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;

public abstract class Task implements Runnable {

	private Logger logger = Logger.getLogger(Task.class.getName());

	private String key;
	private String processKey;
	private int processStep;
	private Map<String, String> params;
	private RuntimeService runtime;
	private StringBuffer log;
	private TaskState state;

	public Task() {
		log = new StringBuffer();
		params = new HashMap<String, String>();
		state = TaskState.CREATED;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	
	public String getParam(String key) {
		return params.get(key);
	}
	
	public void setParam(String key, String value) {
		params.put(key, value);
	}

	public String getProcessKey() {
		return processKey;
	}

	public void setProcessKey(String processKey) {
		this.processKey = processKey;
	}

	public int getProcessStep() {
		return processStep;
	}

	public void setProcessStep(int processStep) {
		this.processStep = processStep;
	}

	public void log(String message) {
		log.append("[").append(key).append("] ").append(message).append("\r\n");
	}

	public String getLog() {
		return log.toString();
	}
	
	public TaskState getState() {
		return state;
	}

	public RuntimeService getRuntime() throws Exception {
		if (runtime == null) {
			runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
		}
		return runtime;
	}
	
	protected void setRuntime(RuntimeService runtime) {
		this.runtime = runtime;
	}

	@Override
	public void run() {
		logger.log(Level.FINE, "starting task: " + getKey());
		state = TaskState.IN_PROGRESS;
		state = execute();
		try {
			getRuntime().notifyTaskExecution(this);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to notify process of task execution", e);
		}
	}
	
	public abstract TaskState execute();

}
