package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class CreateMetadataTask extends RuntimeEngineTask {
	
	public static final String NAME = "Create Bag Objects";
	public static final String WSKEY_PARAM_NAME = "wskey";
	public static final String METADATA_ENTRIES_PARAM_NAME = "metadataentries";

	private static final Logger logger = Logger.getLogger(CreateMetadataTask.class.getName());

	public CreateMetadataTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WSKEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WSKEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WSKEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(METADATA_ENTRIES_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + METADATA_ENTRIES_PARAM_NAME + " is not set");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> entries = execution.getVariable(METADATA_ENTRIES_PARAM_NAME, Map.class);
		
		logger.log(Level.INFO, "Starting metadata objects import for provided entries");
		for (Entry<String, String> entry : entries.entrySet()) {
			logger.log(Level.FINE, "treating metadata entry: " + entry.getKey());
			int lastPathIndex = entry.getKey().lastIndexOf("/");
			String mdfullname = entry.getKey();
			String mdname = mdfullname;
			String mdformat = "unknown";
			String mdpath = "/";
			if (lastPathIndex > -1) {
				mdfullname = entry.getKey().substring(lastPathIndex + 1);
				mdpath = entry.getKey().substring(0, lastPathIndex);
			}
			if (mdfullname.indexOf("[") == 0 && mdfullname.indexOf("]") >= 0) {
				mdformat = mdfullname.substring(1, mdfullname.indexOf("]")).trim();
				mdname = mdfullname.substring(mdfullname.indexOf("]") + 1).trim();
			}
			try {
				logger.log(Level.FINE, "creating metadata object for path: " + mdpath + " with name: " + mdname + " and format: " + mdformat);
				getCoreService().createMetadataObject(wskey, mdpath, mdname, mdformat, entry.getValue());
			} catch (Exception e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating metadata object for path [" + mdpath + "] and name [" + mdname + "]"));
				logger.log(Level.SEVERE, "- error creating metadata for path: " + mdpath + " and name: " + mdname, e);
			}
		}
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

