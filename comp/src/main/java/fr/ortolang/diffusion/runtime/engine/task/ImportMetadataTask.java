package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class ImportMetadataTask extends RuntimeEngineTask {
	
	public static final String NAME = "Import Metadata";

	private static final Logger logger = Logger.getLogger(ImportMetadataTask.class.getName());

	public ImportMetadataTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(METADATA_ENTRIES_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + METADATA_ENTRIES_PARAM_NAME + " is not set");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> entries = execution.getVariable(METADATA_ENTRIES_PARAM_NAME, Map.class);
		
		logger.log(Level.INFO, "Starting metadata objects import for provided entries");
		long start = System.currentTimeMillis();
		int cpt = 0;
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
				try {
					getCoreService().resolveWorkspaceMetadata(wskey, Workspace.HEAD, mdpath, mdname);
					logger.log(Level.FINE, "updating metadata object for path: " + mdpath + " and name: " + mdname);
					getCoreService().updateMetadataObject(wskey, mdpath, mdname, mdformat, entry.getValue());
				} catch ( InvalidPathException e ) {
					logger.log(Level.FINE, "creating metadata object for path: " + mdpath + " and name: " + mdname);
					getCoreService().createMetadataObject(wskey, mdpath, mdname, mdformat, entry.getValue());
					cpt++;
				}
			} catch (Exception e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating metadata object for path [" + mdpath + "] and name [" + mdname + "]"));
				logger.log(Level.SEVERE, "- error creating metadata for path: " + mdpath + " and name: " + mdname, e);
			}
		}
		long stop = System.currentTimeMillis();
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), cpt + " metadata objects created successfully in " + (stop - start) + " ms"));
		

		//TODO crawl the workspace head in order to delete objects that are not in the entry map....
		//TODO crawl the workspace head in order to delete empty collections
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

