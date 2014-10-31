package fr.ortolang.diffusion.runtime.engine.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;

import fr.ortolang.diffusion.runtime.entity.Process.State;

public class ActivitiProcessRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiProcessRunner.class.getName());

	private RuntimeService runtime;
	private String type;
	private String key;
	private Map<String, Object> variables;
	
	public ActivitiProcessRunner(RuntimeService runtime, String type, String key, Map<String, Object> variables) {
		this.runtime = runtime;
		this.type = type;
		this.key = key;
		this.variables = variables;
	}
	
	@Override
	public void run() {
		logger.log(Level.INFO, "ActivitiProcessRunner starting new instance of type " + type  + " with business key: " + key);
		try {
			ActivitiEvent event = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_STATE_UPDATE", State.RUNNING, "", key, "");
			runtime.dispatchEvent(event);
			runtime.startProcessInstanceByKey(type, key, variables);			
		} catch ( ActivitiObjectNotFoundException e ) {
			logger.log(Level.WARNING, "Error during starting process instance", e);
			//TODO Maybe log reason of start failure in process log
			ActivitiEvent event = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_STATE_UPDATE", State.ABORTED, "", key, "");
			runtime.dispatchEvent(event);
		}
		logger.log(Level.INFO, "ActivitiProcessRunner stopped for business key: " + key);
	}

}
