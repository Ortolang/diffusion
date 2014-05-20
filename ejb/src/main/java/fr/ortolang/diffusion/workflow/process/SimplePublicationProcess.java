package fr.ortolang.diffusion.workflow.process;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.publication.PublicationService;

public class SimplePublicationProcess extends ProcessExecutionTask {
	
	private Logger logger = Logger.getLogger(SimplePublicationProcess.class.getName());
	
	private PublicationService publication;
	
	public SimplePublicationProcess() {
	}
	
	public PublicationService getPublicationService() throws Exception {
		if ( publication == null ) {
			publication = (PublicationService) OrtolangServiceLocator.findService("publication");
		}
		return publication;
	}
	
	@Override
	public void process() throws Exception {
		this.log("Starting simple publication process");
		if ( !this.getParams().containsKey("keys") ) {
			logger.log(Level.WARNING, "unable to find 'keys' parameter");
			throw new Exception("mandatory parameter 'keys' not present, unable to execute task");
		} else {
			logger.log(Level.INFO, "'keys' parameter found: " + this.getParams().get("keys"));
			List<String> keys = Arrays.asList(this.getParams().get("keys").split(","));
			for (String key : keys) {
				try {
					getPublicationService().publishForAll(key);
					this.log("key [" + key + "] published successfully");
				} catch ( Exception e ) {
					this.log("key [" + key + "] failed to publish: " + e.getMessage());
				}
			}
		}
		this.log("Simple publication process done.");
	}

}
