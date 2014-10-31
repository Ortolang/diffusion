package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.publication.ForAllPublicationType;
import fr.ortolang.diffusion.publication.PublicationService;

public class PublishKeysTask implements JavaDelegate {

	private static final Logger logger = Logger.getLogger(PublishKeysTask.class.getName());
	
	public static final String KEYS = "keys";
	public static final String STATUS = "status";
	
	private PublicationService publication;

	public PublishKeysTask() {
	}

	public PublicationService getPublicationService() throws Exception {
		if (publication == null) {
			publication = (PublicationService) OrtolangServiceLocator.findService("publication");
		}
		return publication;
	}
	
	public void setPublicationService(PublicationService publication) {
		this.publication = publication;
	}

	@Override
	public void execute(DelegateExecution execution) {
		logger.log(Level.INFO, "Starting publication task");
		try {
			if (!execution.hasVariable(KEYS)) {
				logger.log(Level.WARNING, "unable to find mandatory " + KEYS + " variable");
				execution.createVariableLocal(STATUS, "failed");
			} else {
				logger.log(Level.INFO, KEYS + " parameter found");
				List<String> keys = Arrays.asList(execution.getVariable(KEYS, String.class).split(","));
				for (String key : keys) {
					try {
						getPublicationService().publish(key, new ForAllPublicationType());
						logger.log(Level.INFO, "key [" + key + "] published successfully");
					} catch (Exception e) {
						logger.log(Level.INFO, "key [" + key + "] failed to publish: " + e.getMessage());
					}
				}
				logger.log(Level.INFO, "Publication task ended");
				execution.createVariableLocal(STATUS, "completed");
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error during task execution" + e);
			execution.createVariableLocal(STATUS, "error");
		}
	}

}