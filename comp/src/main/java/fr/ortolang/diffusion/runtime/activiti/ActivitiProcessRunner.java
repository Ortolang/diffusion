package fr.ortolang.diffusion.runtime.activiti;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RuntimeService;

public class ActivitiProcessRunner implements Runnable {

	private Logger logger = Logger.getLogger(ActivitiProcessRunner.class.getName());

	private RuntimeService runtime;
	private String definitionId;
	private String businessKey;
	private Map<String, Object> variables;
	
	public ActivitiProcessRunner(RuntimeService runtime, String definitionId, String businessKey, Map<String, Object> variables) {
		this.runtime = runtime;
		this.definitionId = definitionId;
		this.businessKey = businessKey;
		this.variables = variables;
	}

	@Override
	public void run() {
		logger.log(Level.INFO, "ActivitiProcessRunner starting new instance of process definition id " + definitionId  + " with business key: " + businessKey);
		try {
			runtime.startProcessInstanceById(definitionId, businessKey, variables);			
		} catch ( ActivitiObjectNotFoundException e ) {
			logger.log(Level.WARNING, "Error during starting process instance", e);
		}
		logger.log(Level.INFO, "ActivitiProcessRunner stopped for business key: " + businessKey);
	}

}
