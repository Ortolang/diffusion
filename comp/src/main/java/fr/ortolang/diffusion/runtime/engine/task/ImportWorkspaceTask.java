package fr.ortolang.diffusion.runtime.engine.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class ImportWorkspaceTask extends RuntimeEngineTask {
	
	private static final Logger logger = Logger.getLogger(ImportWorkspaceTask.class.getName());
	
	public static final String NAME = "Import Workspace";
	public static final String DATA_PREFIX = "data/";
	public static final String HEAD_PREFIX = "data/head/";
	public static final String SNAPSHOTS_PREFIX = "data/snapshots/";
	public static final String OBJECTS_PREFIX = "objects/";
	public static final String METADATA_PREFIX = "metadata/";
	
	public ImportWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		logger.log(Level.INFO, "Starting Import Workspace Task");
		
		logger.log(Level.FINE, "- read parameters");
		checkParameters(execution);
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		String wsname = execution.getVariable(WORKSPACE_NAME_PARAM_NAME, String.class);
		String wstype = execution.getVariable(WORKSPACE_TYPE_PARAM_NAME, String.class);
		String wsalias = execution.getVariable(WORKSPACE_ALIAS_PARAM_NAME, String.class);
		
		logger.log(Level.FINE, "- load and check bag");
		Bag bag = loadAndCheckBag(bagpath);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag loaded from local file: " + bag.getFile()));
		
		logger.log(Level.FINE, "- list versions from bag");
		List<String> versions = listVersions(bag);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), versions.size() + " versions found in this bag: " + Arrays.deepToString(versions.toArray())));
		
		logger.log(Level.FINE, "- create workspace");
		createWorkspace(wskey, wsalias, wsname, wstype);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace created with key: " + wskey));
		
		Set<Path> previousObjects = Collections.emptySet();
		Set<Path> previousMetadata = Collections.emptySet();
		for ( String version : versions ) {
			logger.log(Level.FINE, "- start import of version " + version);
			logger.log(Level.FINE, "- built version objects tree");
			Set<Path> objects = listObjects(version, bag.getPayload());
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Objects found in version " + version + ": " + Arrays.deepToString(objects.toArray())));
			
			Set<Path> objectsToCreate = new HashSet<Path> ();
			objectsToCreate.addAll(objects);
			objectsToCreate.removeAll(previousObjects);
			Set<Path> objectsToUpdate = new HashSet<Path> ();
			objectsToUpdate.addAll(objects);
			objectsToUpdate.retainAll(previousObjects);
			Set<Path> objectsToDelete = new HashSet<Path> ();
			objectsToDelete.addAll(previousObjects);
			objectsToDelete.removeAll(objectsToUpdate);
			previousObjects = objects;
			logger.log(Level.INFO, "[OBJECT-CREATE] " + Arrays.deepToString(objectsToCreate.toArray()));
			logger.log(Level.INFO, "[OBJECT-UPDATE] " + Arrays.deepToString(objectsToUpdate.toArray()));
			logger.log(Level.INFO, "[OBJECT-DELETE] " + Arrays.deepToString(objectsToDelete.toArray()));
						
			logger.log(Level.FINE, "- built version metadata tree");
			Set<Path> metadata = listMetadata(version, bag.getPayload());
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Metadata found in version " + version + ": " + Arrays.deepToString(metadata.toArray())));
			
			Set<Path> metadataToCreate = new HashSet<Path> ();
			metadataToCreate.addAll(metadata);
			metadataToCreate.removeAll(previousMetadata);
			Set<Path> metadataToUpdate = new HashSet<Path> ();
			metadataToUpdate.addAll(metadata);
			metadataToUpdate.retainAll(previousMetadata);
			Set<Path> metadataToDelete = new HashSet<Path> ();
			metadataToDelete.addAll(previousMetadata);
			metadataToDelete.removeAll(metadataToUpdate);
			previousMetadata = metadata;
			logger.log(Level.INFO, "[METADATA-CREATE] " + Arrays.deepToString(metadataToCreate.toArray()));
			logger.log(Level.INFO, "[METADATA-UPDATE] " + Arrays.deepToString(metadataToUpdate.toArray()));
			logger.log(Level.INFO, "[METADATA-DELETE] " + Arrays.deepToString(metadataToDelete.toArray()));
		}
		
	}

	@Override
	public String getTaskName() {
		return NAME;
	}
	
	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_NAME_PARAM_NAME + " is not set");
		}
		if ( !execution.hasVariable(WORKSPACE_TYPE_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_TYPE_PARAM_NAME, "unknown");
		}
		
	}
	
	private Bag loadAndCheckBag(Path bagpath) throws RuntimeEngineTaskException {
		if ( !Files.exists(bagpath) ) {
			throw new RuntimeEngineTaskException("bag file " + bagpath + " does not exists");
		}
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		SimpleResult result = bag.verifyPayloadManifests();
		if (!result.isSuccess()) {
			logger.log(Level.WARNING, "bag verification failed: " + result.messagesToString());
			throw new RuntimeEngineTaskException("bag verification failed: " + result.messagesToString());
		}
		return bag;
	}
	
	private List<String> listVersions(Bag bag) {
		Collection<BagFile> payload = bag.getPayload();
		Map<Integer, String> snapshots = new HashMap<Integer, String> ();
		for (BagFile bagfile : payload) {
			if ( bagfile.getFilepath().startsWith(SNAPSHOTS_PREFIX) ) {
				String[] parts = bagfile.getFilepath().substring(SNAPSHOTS_PREFIX.length()).split("/");
				if ( parts.length <= 1 || parts[0].length() <= 0 || parts[1].length() <= 0 ) {
					logger.log(Level.INFO, "Unparsable snapshot hierarchy found: " + Arrays.deepToString(parts));
				}
				Integer index = -1;
				try {
					index = Integer.decode(parts[0]);
					if ( snapshots.containsKey(index) ) {
						if ( !snapshots.get(index).equals(parts[1]) ) {
							logger.log(Level.WARNING, "Found a version with existing index but different name!! " + snapshots.get(index) + " - " + parts[1]);
						}
					} else {
						logger.log(Level.INFO, "Found new version with index: " + index + " and name: " + parts[1]);
						snapshots.put(index, parts[1]);
					}
				} catch ( Exception e ) {
					logger.log(Level.INFO, "Snapshot index is not a number: " + parts[0]);
				}
			}
		}
	
		List<String> versions = new ArrayList<String> ();
		String snapshot = null;
		Integer cpt = 1;
		while ( (snapshot = snapshots.get(cpt)) != null ) {
			versions.add("snapshots/" + cpt + "/" + snapshot);
			cpt++;
		}
		versions.add(Workspace.HEAD);
		return versions;
	}
	
	private void createWorkspace(String key, String alias, String name, String type) throws RuntimeEngineTaskException {
		try {
			OrtolangObjectIdentifier wsidentifier = getRegistryService().lookup(key);
			if ( !wsidentifier.getService().equals(CoreService.SERVICE_NAME) && !wsidentifier.getType().equals(Workspace.OBJECT_TYPE) ) {
				logger.log(Level.SEVERE, "Workspace Key already exists but is not a workspace !!");
				throw new RuntimeEngineTaskException("Workspace Key already exists but is NOT a workspace !!");
			}
		} catch ( KeyNotFoundException e ) {
			try {
				if ( alias != null ) {
					getCoreService().createWorkspace(key, alias, name, type);
				} else {
					getCoreService().createWorkspace(key, name, type);
				}
			} catch ( Exception e2 ) {
				logger.log(Level.SEVERE, "unable to create workspace", e2);
				throw new RuntimeEngineTaskException("unable to create workspace", e2);
			}
		} catch (RegistryServiceException e) {
			throw new RuntimeEngineTaskException("unable to create workspace", e);
		}
	}
	
	private Set<Path> listObjects(String version, Collection<BagFile> payload) {
		Set<Path> objects = new HashSet<Path>();
		String prefix = DATA_PREFIX + version;
		if ( !prefix.endsWith("/") ) {
			prefix += "/";
		}
		for (BagFile file : payload) {
			if (file.getFilepath().startsWith(prefix + OBJECTS_PREFIX)) {
				objects.add(Paths.get(file.getFilepath().substring((prefix + OBJECTS_PREFIX).length()-1)));
			} 
		}
		return objects;
	}
	
	private Set<Path> listMetadata(String version, Collection<BagFile> payload) {
		Set<Path> metadata = new HashSet<Path>();
		String prefix = DATA_PREFIX + version;
		if ( !prefix.endsWith("/") ) {
			prefix += "/";
		}
		for (BagFile file : payload) {
			if (file.getFilepath().startsWith(prefix + METADATA_PREFIX)) {
				metadata.add(Paths.get(file.getFilepath().substring((prefix + METADATA_PREFIX).length()-1)));
			} 
		}
		return metadata;
	}
	
}