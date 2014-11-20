package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineException;

public class ActivitiProcessRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiProcessRunner.class.getName());

	private ActivitiEngineBean engine;
	private String type;
	private String key;
	private Map<String, Object> variables;
	
	public ActivitiProcessRunner(ActivitiEngineBean engine, String type, String key, Map<String, Object> variables) {
		this.engine = engine;
		this.type = type;
		this.key = key;
		this.variables = variables;
	}
	
	@Override
	public void run() {
		logger.log(Level.INFO, "ActivitiProcessRunner starting new instance of type " + type  + " with business key: " + key);
		try {
			try {
				engine.notify(RuntimeEngineEvent.createProcessStartEvent(key));
				engine.getActivitiRuntimeService().startProcessInstanceByKey(type, key, variables);			
			} catch ( ActivitiObjectNotFoundException e ) {
				logger.log(Level.WARNING, "Error during starting process instance", e);
				engine.notify(RuntimeEngineEvent.createProcessAbortEvent(key, e.getMessage()));
			}
		} catch ( RuntimeEngineException e ) {
			logger.log(Level.WARNING, "unexpected error during process runner execution", e);
		}
		logger.log(Level.INFO, "ActivitiProcessRunner stopped for business key: " + key);
	}

}
