package fr.ortolang.diffusion.runtime.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RuntimeService;

public class ActivitiProcessRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiProcessRunner.class.getName());

	private ActivitiEngineListener listener;
	private RuntimeService runtime;
	private String type;
	private String key;
	private Map<String, Object> variables;
	
	public ActivitiProcessRunner(ActivitiEngineListener listener, RuntimeService runtime, String type, String key, Map<String, Object> variables) {
		this.listener = listener;
		this.runtime = runtime;
		this.type = type;
		this.key = key;
		this.variables = variables;
	}
	
	@Override
	public void run() {
		logger.log(Level.INFO, "ActivitiProcessRunner starting new instance of type " + type  + " with business key: " + key);
		try {
			//TODO Change direct listener call to an event
			listener.onProcessStart(key);
			runtime.startProcessInstanceByKey(type, key, variables);			
		} catch ( ActivitiObjectNotFoundException e ) {
			logger.log(Level.WARNING, "Error during starting process instance", e);
			listener.onProcessError(key, e.getMessage());
		}
		logger.log(Level.INFO, "ActivitiProcessRunner stopped for business key: " + key);
	}

}
