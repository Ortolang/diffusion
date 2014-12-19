package fr.ortolang.diffusion.runtime.engine.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class CheckBagTask extends RuntimeEngineTask {
	
	public static final String NAME = "Check Bag Integrity";
	
	private static final Logger logger = Logger.getLogger(CheckBagTask.class.getName());

	public CheckBagTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		if ( !Files.exists(bagpath) ) {
			throw new RuntimeEngineTaskException("bag file " + bagpath + " does not exists");
		}
		
		logger.log(Level.FINE, "loading bag from file: " + bagpath);
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		logger.log(Level.FINE, "bag loaded: " + bag.getBagItTxt());
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag loaded from local file: " + bagpath));
		
		logger.log(Level.FINE, "verifying bag content integrity...");
		long start = System.currentTimeMillis();
		SimpleResult result = bag.verifyPayloadManifests();
		if (!result.isSuccess()) {
			logger.log(Level.WARNING, "bag verification failed: " + result.messagesToString());
			throw new RuntimeEngineTaskException("bag verification failed: " + result.messagesToString());
		}
		long stop = System.currentTimeMillis();
		try {
			bag.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close bag", e);
		}
		logger.log(Level.FINE, "bag verification success done in " + (stop - start) + " ms");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag integrity checked successfully from local file: " + bagpath + " in " + (stop - start) + " ms"));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

