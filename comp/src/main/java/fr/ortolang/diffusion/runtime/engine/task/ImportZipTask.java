package fr.ortolang.diffusion.runtime.engine.task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

public class ImportZipTask extends RuntimeEngineTask {
	private static final Logger LOGGER = Logger.getLogger(ImportZipTask.class.getName());

	public static final String NAME = "Import Zip Content";

	public ImportZipTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		LOGGER.log(Level.INFO, "Starting Import Content Task");
		checkParameters(execution);
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		Path zippath = Paths.get(execution.getVariable(ZIP_PATH_PARAM_NAME, String.class));
		String root = execution.getVariable(ZIP_ROOT_PARAM_NAME, String.class);
		PathBuilder rootPath;
		try {
			rootPath = PathBuilder.fromPath(root);
		} catch (InvalidPathException e) {
			throw new RuntimeEngineTaskException("parameter " + ZIP_ROOT_PARAM_NAME + " value " + root + " is not a valid path");
		}
		boolean overwrite = false;
		if (execution.hasVariable(ZIP_OVERWRITE_PARAM_NAME)) {
			overwrite = Boolean.parseBoolean(execution.getVariable(ZIP_OVERWRITE_PARAM_NAME, String.class));
		}
		LOGGER.log(Level.FINE, "- starting import zip");
		try {
			Set<String> cache = new HashSet<String>();
			ZipFile zip = new ZipFile(zippath.toFile());
			boolean partial = false;
			for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				try {
					if (!entry.isDirectory()) {
						PathBuilder opath = rootPath.clone().path(entry.getName());
						try {
							String okey = getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, opath.build());
							if ( overwrite ) {
								LOGGER.log(Level.FINE, " updating object at path: " + opath);
								try {
									InputStream is = zip.getInputStream(entry);
									String hash = getCoreService().put(is);
									is.close();
									DataObject object = getCoreService().readDataObject(okey);
									getCoreService().updateDataObject(wskey, opath.build(), object.getDescription(), hash);
								} catch ( InvalidPathException | DataCollisionException | KeyNotFoundException e4 ) {
									partial = true;
								}
							}
						} catch ( InvalidPathException e3 ) {
							LOGGER.log(Level.FINE, " creating object at path: " + opath);
							try {
								InputStream is = zip.getInputStream(entry);
								String hash = getCoreService().put(is);
								is.close();
								PathBuilder oppath = opath.clone().parent();
								if (!oppath.isRoot() && !cache.contains(oppath.build())) {
									String[] parents = oppath.parent().buildParts();
									String current = "";
									for (int i = 0; i < parents.length; i++) {
										current += "/" + parents[i];
										if (!cache.contains(current)) {
											try {
												getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
											} catch (InvalidPathException e2) {
												getCoreService().createCollection(wskey, current, "");
											}
											cache.add(current);
										}
									}
								}
								String current = opath.build();
								getCoreService().createDataObject(wskey, current, "", hash);
							} catch ( InvalidPathException | DataCollisionException | KeyNotFoundException e4 ) {
								partial = true;
							}
						} 
					}
				} catch (InvalidPathException | CoreServiceException | AccessDeniedException e2) {
					partial = true;
				}
			}
			zip.close();
			if ( partial ) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some objects has not been imported"));
			}
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Zip content done"));
		} catch (IOException e1) {
			throw new RuntimeEngineTaskException("error reading zip file: " + e1.getMessage());
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		if (!execution.hasVariable(ZIP_PATH_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + ZIP_PATH_PARAM_NAME + " is not set");
		}
		if (!execution.hasVariable(ZIP_ROOT_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + ZIP_ROOT_PARAM_NAME + " is not set");
		}
	}

}
