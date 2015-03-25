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

import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest.Algorithm;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class LoadBagContentTask extends RuntimeEngineTask {
	
	private static final Logger logger = Logger.getLogger(LoadBagContentTask.class.getName());
	
	public static final String NAME = "Load Bag Content";
	public static final String DATA_PREFIX = "data/";
	public static final String HEAD_PREFIX = "data/head/";
	public static final String SNAPSHOTS_PREFIX = "data/snapshots/";
	public static final String OBJECTS_PREFIX = "objects/";
	public static final String METADATA_PREFIX = "metadata/";
	
	public LoadBagContentTask() {
	}
	
	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		logger.log(Level.INFO, "Starting Load Bag Content Task");
		checkParameters(execution);
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		Bag bag = loadBag(bagpath);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag file loaded from local file: " + bag.getFile()));
		
		logger.log(Level.FINE, "- list versions");
		List<String> versions = searchVersions(bag);
		
		logger.log(Level.FINE, "- build import script");
		List<String> operations = new ArrayList<String> ();
		String pversion = null;
		Set<String> pobjects = Collections.emptySet();
		Set<String> pmetadata = Collections.emptySet();
		for (String version : versions) {
			Set<String> objects = listObjects(version, bag.getPayload());
			operations.addAll(buildObjectsOperations(bag, pversion, version, pobjects, objects));
			Set<String> metadata = listMetadata(version, bag.getPayload());
			operations.addAll(buildMetadataOperations(bag, pversion, version, pmetadata, metadata));
			if (!version.equals(Workspace.HEAD)) {
				operations.add("snapshot-workspace\t" + version.substring(version.lastIndexOf("/")));
			}
			pversion = version;
			pobjects = objects;
			pmetadata = metadata;
		}
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import script generated"));
		
		for ( String operation : operations ) {
			logger.log(Level.INFO, operation);
		}
	}
	
	@Override
	public String getTaskName() {
		return NAME;
	}

	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(BAG_PATH_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
	}
	
	private Bag loadBag(Path bagpath) throws RuntimeEngineTaskException {
		if (!Files.exists(bagpath)) {
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
	
	private List<String> searchVersions(Bag bag) {
		Collection<BagFile> payload = bag.getPayload();
		Map<Integer, String> snapshots = new HashMap<Integer, String>();
		for (BagFile bagfile : payload) {
			if (bagfile.getFilepath().startsWith(SNAPSHOTS_PREFIX)) {
				String[] parts = bagfile.getFilepath().substring(SNAPSHOTS_PREFIX.length()).split("/");
				if (parts.length <= 1 || parts[0].length() <= 0 || parts[1].length() <= 0) {
					logger.log(Level.INFO, "Unparsable snapshot hierarchy found: " + Arrays.deepToString(parts));
				}
				Integer index = -1;
				try {
					index = Integer.decode(parts[0]);
					if (snapshots.containsKey(index)) {
						if (!snapshots.get(index).equals(parts[1])) {
							logger.log(Level.WARNING, "Found a version with existing index but different name!! " + snapshots.get(index) + " - " + parts[1]);
						}
					} else {
						logger.log(Level.INFO, "Found new version with index: " + index + " and name: " + parts[1]);
						snapshots.put(index, parts[1]);
					}
				} catch (Exception e) {
					logger.log(Level.INFO, "Snapshot index is not a number: " + parts[0]);
				}
			}
		}

		List<String> versions = new ArrayList<String>();
		String snapshot = null;
		Integer cpt = 1;
		while ((snapshot = snapshots.get(cpt)) != null) {
			versions.add("snapshots/" + cpt + "/" + snapshot);
			cpt++;
		}
		versions.add(Workspace.HEAD);
		return versions;
	}
	
	private Set<String> listObjects(String version, Collection<BagFile> payload) {
		Set<String> objects = new HashSet<String>();
		String prefix = DATA_PREFIX + version;
		if (!prefix.endsWith("/")) {
			prefix += "/";
		}
		for (BagFile file : payload) {
			if (file.getFilepath().startsWith(prefix + OBJECTS_PREFIX)) {
				objects.add(file.getFilepath().substring((prefix + OBJECTS_PREFIX).length() - 1));
			}
		}
		return objects;
	}
	
	private List<String> buildObjectsOperations(Bag bag, String pversion, String version, Set<String> pobjects, Set<String> objects) {
		List<String> operations = new ArrayList<String> ();
		for (String object : objects) {
			if ( !pobjects.contains(object) ) {
				String bagfilepath = DATA_PREFIX + version + "/objects" + object;
				operations.add("create-object\t" + bagfilepath + "\t" + bag.getChecksums(bagfilepath).get(Algorithm.SHA1) + "\t" + object);
			} else {
				String bagfilepath = DATA_PREFIX + version + "/objects" + object;
				BagFile file = bag.getBagFile(bagfilepath);
				String filehash = bag.getChecksums(bagfilepath).get(Algorithm.SHA1);
				boolean skipupdate = false;
				if ( pversion != null ) {
					String oldfilepath = DATA_PREFIX + pversion + "/objects" + object;
					BagFile oldfile = bag.getBagFile(oldfilepath);
					String oldfilehash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
					if ( oldfilehash != null && oldfile.getSize() == file.getSize() && oldfilehash.equals(filehash) ) {
						skipupdate = true;
					}
				} 
				if ( !skipupdate ) {
					operations.add("update-object\t" + bagfilepath + "\t" + object);
				}
			}
		}
		for (String object : pobjects) {
			if ( !objects.contains(object) ) {
				String bagfilepath = DATA_PREFIX + version + "/objects" + object;
				operations.add("delete-object\t" + bagfilepath + "\t" + object);
			}
		}
		return operations;
	}
	
	private Set<String> listMetadata(String version, Collection<BagFile> payload) {
		Set<String> metadata = new HashSet<String>();
		String prefix = DATA_PREFIX + version;
		if (!prefix.endsWith("/")) {
			prefix += "/";
		}
		for (BagFile file : payload) {
			if (file.getFilepath().startsWith(prefix + METADATA_PREFIX)) {
				metadata.add(file.getFilepath().substring((prefix + METADATA_PREFIX).length() - 1));
			}
		}
		return metadata;
	}
	
	private List<String> buildMetadataOperations(Bag bag, String pversion, String version, Set<String> pmetadata, Set<String> metadata) {
		List<String> operations = new ArrayList<String> ();
		for (String md : metadata) {
			if ( !pmetadata.contains(md) ) {
				String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
				operations.add("create-metadata\t" + bagfilepath + "\t" + md);
			} else {
				String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
				BagFile file = bag.getBagFile(bagfilepath);
				String hash = bag.getChecksums(bagfilepath).get(Algorithm.SHA1);
				boolean skipupdate = false;
				if ( pversion != null ) {
					String oldfilepath = DATA_PREFIX + pversion + "/metadata" + md;
					BagFile oldfile = bag.getBagFile(oldfilepath);
					String oldhash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
					if ( oldhash != null && oldfile.getSize() == file.getSize() && oldhash.equals(hash) ) {
						skipupdate = true;
					}
				} 
				if ( !skipupdate ) {
					operations.add("update-metadata\t" + bagfilepath + "\t" + md);
				}
			}
		}
		for (String md : pmetadata) {
			if ( !metadata.contains(md) ) {
				String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
				operations.add("delete-metadata\t" + bagfilepath + "\t" + md);
			}
		}
		return operations;
	}
	
}
