package fr.ortolang.diffusion.runtime.engine.task;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;

public class ListBagVersionsTask extends RuntimeEngineTask {
	
	public static final String NAME = "List Bag Versions";
	public static final String BAG_PATH_PARAM_NAME = "bagpath";
	public static final String BAG_VERSIONS_PARAM_NAME = "bagversions";
	public static final String HEAD_PREFIX = "data/head";
	public static final String SNAPSHOTS_PREFIX = "data/snapshots";

	private static final Logger logger = Logger.getLogger(ListBagVersionsTask.class.getName());

	public ListBagVersionsTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		
		logger.log(Level.INFO, "listing bag versions");
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		Collection<BagFile> payload = bag.getPayload();
		List<String> versions = new ArrayList<String> ();
		for (BagFile bagfile : payload) {
			if ( bagfile.getFilepath().startsWith(SNAPSHOTS_PREFIX) ) {
				String[] parts = bagfile.getFilepath().substring(SNAPSHOTS_PREFIX.length()).split("/");
				if ( parts.length <= 1 ) {
					logger.log(Level.INFO, "Unparsable snapshot hierarchy found: " + Arrays.deepToString(parts));
				}
				logger.log(Level.INFO, "Found version with index: " + parts[0] + " and name: " + parts[1]);
			}
		}
		versions.add("head");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), versions.size() + " versions found in this bag: " + Arrays.deepToString(versions.toArray())));
		
		execution.setVariable(BAG_VERSIONS_PARAM_NAME, versions);
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	@Override
	public boolean needEngineAuth() {
		return false;
	}

}

