package fr.ortolang.diffusion.runtime.task;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.publication.ForAllPublicationType;
import fr.ortolang.diffusion.publication.PublicationService;

public class PublicationTask extends Task {
	
	public static final String KEYS_PARAM = "keys";
	
	private Logger logger = Logger.getLogger(PublicationTask.class.getName());
	private PublicationService publication;
	
	public PublicationTask() {
		super();
	}
	
	public PublicationService getPublicationService() throws Exception {
		if ( publication == null ) {
			publication = (PublicationService) OrtolangServiceLocator.findService("publication");
		}
		return publication;
	}
	
	@Override
	public TaskState execute() {
		this.log("Starting publication task");
		try {
			if ( !getParams().containsKey(KEYS_PARAM) ) {
				logger.log(Level.WARNING, "unable to find mandatory " + KEYS_PARAM + " parameter");
				this.log("Publication task error : mandatory parameter " + KEYS_PARAM + " not found in context!!");
				return TaskState.ERROR;
			} else {
				logger.log(Level.INFO, "'keys' parameter found: " + getParam(KEYS_PARAM));
				List<String> keys = Arrays.asList(getParam(KEYS_PARAM).split(","));
				for (String key : keys) {
					try {
						getPublicationService().publish(key, new ForAllPublicationType());
						this.log("key [" + key + "] published successfully");
					} catch ( Exception e ) {
						this.log("key [" + key + "] failed to publish: " + e.getMessage());
					}
				}
			}
			this.log("Publication task ended");
			return TaskState.COMPLETED;
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "error during task execution" + e);
			this.log("unexpected error occured during task execution: " + e.getMessage() + ", see server log for further details");
			return TaskState.ERROR;
		}
	}

}
