package fr.ortolang.diffusion.runtime.engine.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class ImportObjectsTask extends RuntimeEngineTask {
	
	public static final String NAME = "Import Data Objects";
	
	private static final Logger logger = Logger.getLogger(ImportObjectsTask.class.getName());

	public ImportObjectsTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(OBJECT_ENTRIES_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + OBJECT_ENTRIES_PARAM_NAME + " is not set");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> entries = execution.getVariable(OBJECT_ENTRIES_PARAM_NAME, Map.class);
		
		logger.log(Level.INFO, "Starting data objects import for provided entries");
		List<String> collections = new ArrayList<String>();
		long start = System.currentTimeMillis();
		int cpt = 0;
		for (Entry<String, String> entry : entries.entrySet()) {
			logger.log(Level.FINE, "treating imported entry: " + entry.getKey());
			try {
				PathBuilder opath = PathBuilder.fromPath(entry.getKey());
				PathBuilder oppath = opath.clone().parent();
				if (!oppath.isRoot() && !collections.contains(oppath.build())) {
					String[] parents = opath.clone().parent().buildParts();
					String current = "";
					for (int i = 0; i < parents.length; i++) {
						current += "/" + parents[i];
						if (!collections.contains(current)) {
							try {
								try {
									getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
									logger.log(Level.FINE, "collection already exists for path: " + current);
								} catch ( InvalidPathException e ) {
									logger.log(Level.FINE, "creating collection for path: " + current);
									getCoreService().createCollection(wskey, current, "");
								}
								collections.add(current);
							} catch (Exception e) {
								throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating collection at path [" + current + "]"));
								logger.log(Level.SEVERE, "error creating collection at path: " + current + ", should result in data object creation error", e);
							}
						}
					}
				}
				String current = opath.build();
				try {
					try {
						getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
						logger.log(Level.FINE, "updating data object for path: " + current);
						getCoreService().updateDataObject(wskey, current, "", entry.getValue());
					} catch ( InvalidPathException e ) {
						logger.log(Level.FINE, "creating data object for path: " + current);
						getCoreService().createDataObject(wskey, current, "", entry.getValue());
						cpt++;
					}
				} catch (Exception e) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating data object at path [" + current + "]"));
					logger.log(Level.SEVERE, "Error creating data object at path: " + current, e);
				}
				
			} catch (fr.ortolang.diffusion.core.InvalidPathException e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating data object at path [" + entry.getKey() + "]"));
				logger.log(Level.SEVERE, "Error creating data object with path: " + entry.getKey() + ", invalid path");
			}
		}
		long stop = System.currentTimeMillis();
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), cpt + " data objects created successfully in " + (stop - start) + " ms"));
		
		
		//TODO crawl the workspace head in order to delete objects that are not in the entry map....
		//TODO crawl the workspace head in order to delete empty collections
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

