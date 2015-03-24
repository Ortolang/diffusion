package fr.ortolang.diffusion.runtime.engine.task;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CollectionNotEmptyException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.template.TemplateEngine;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest.Algorithm;
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
		
		logger.log(Level.FINE, "- load bag");
		checkParameters(execution);
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		Bag bag = loadBag(bagpath);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag loaded from local file: " + bag.getFile()));

		logger.log(Level.FINE, "- create workspace");
		String wskey = createWorkspace(bag);
		logger.log(Level.INFO, "  - workspace created: " + wskey);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace created with key: " + wskey));
		
		logger.log(Level.FINE, "- build global params");
		Map<String, Object> globalparams = new HashMap<String, Object> ();
		loadWorkspaceParams(wskey, globalparams);

		logger.log(Level.FINE, "- list versions");
		List<String> versions = listVersions(bag);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), versions.size() + " versions found in this workspace: " + Arrays.deepToString(versions.toArray())));

		String previousVersion = null;
		Set<String> previousObjects = Collections.emptySet();
		Set<String> previousMetadata = Collections.emptySet();
		for (String version : versions) {
			
			Set<String> objects = listObjects(version, bag.getPayload());
			Set<String> objectsToCreate = new HashSet<String>();
			objectsToCreate.addAll(objects);
			objectsToCreate.removeAll(previousObjects);
			Set<String> objectsToUpdate = new HashSet<String>();
			objectsToUpdate.addAll(objects);
			objectsToUpdate.retainAll(previousObjects);
			Set<String> objectsToDelete = new HashSet<String>();
			objectsToDelete.addAll(previousObjects);
			objectsToDelete.removeAll(objectsToUpdate);
			previousObjects = objects;
			Set<String> cache = new HashSet<String>();
			int created = 0;
			for (String object : objectsToCreate) {
				String filepath = DATA_PREFIX + version + "/objects" + object;
				try {
					logger.log(Level.FINE, "  - create object: " + filepath);
					createObject(wskey, bag.getBagFile(filepath), bag.getChecksums(filepath).get(Algorithm.SHA1), object, cache);
					created++;
				} catch ( InvalidPathException e ) {
					logger.log(Level.SEVERE, "  - OBJECT CREATION ERROR: " + filepath);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR creating object with path " + filepath));
				}
			}
			int updated = 0;
			for (String object : objectsToUpdate) {
				String filepath = DATA_PREFIX + version + "/objects" + object;
				BagFile file = bag.getBagFile(filepath);
				String filehash = bag.getChecksums(filepath).get(Algorithm.SHA1);
				boolean skipupdate = false;
				if ( previousVersion != null ) {
					String oldfilepath = DATA_PREFIX + previousVersion + "/objects" + object;
					BagFile oldfile = bag.getBagFile(oldfilepath);
					String oldfilehash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
					if ( oldfilehash != null && oldfile.getSize() == file.getSize() && oldfilehash.equals(filehash) ) {
						skipupdate = true;
					}
				} 
				if ( !skipupdate ) {
					try {
						logger.log(Level.FINE, "  - update object: " + filepath);
						updateObject(wskey, bag.getBagFile(filepath), object);
						updated++;
					} catch ( InvalidPathException e ) {
						logger.log(Level.SEVERE, "  - OBJECT UPDATE ERROR: " + filepath);
						throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR updating object with path " + filepath));
					}
				}
			}
			int deleted = 0;
			for (String object : objectsToDelete) {
				String filepath = DATA_PREFIX + version + "/objects" + object;
				try {
					logger.log(Level.FINE, "  - delete object: " + filepath);
					deleteObject(wskey, object);
					deleted++;
				} catch ( InvalidPathException e ) {
					logger.log(Level.SEVERE, "  - OBJECT DELETE ERROR: " + filepath);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR deleting object with path " + filepath));
				}
			}
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - objects summary: " + created + " created, " + updated + " updated, " + deleted + " deleted"));

			Set<String> metadata = listMetadata(version, bag.getPayload());
			Set<String> metadataToCreate = new HashSet<String>();
			metadataToCreate.addAll(metadata);
			metadataToCreate.removeAll(previousMetadata);
			Set<String> metadataToUpdate = new HashSet<String>();
			metadataToUpdate.addAll(metadata);
			metadataToUpdate.retainAll(previousMetadata);
			Set<String> metadataToDelete = new HashSet<String>();
			metadataToDelete.addAll(previousMetadata);
			metadataToDelete.removeAll(metadataToUpdate);
			previousMetadata = metadata;
			int mdcreated = 0;
			
			for (String md : metadataToCreate) {
				String filepath = DATA_PREFIX + version + "/metadata" + md;
				try {
					logger.log(Level.FINE, "  - create metadata: " + filepath);
					createMetadata(wskey, bag.getBagFile(filepath), md, globalparams);
					mdcreated++;
				} catch ( InvalidPathException e ) {
					logger.log(Level.SEVERE, "  - METADATA CREATE ERROR: " + filepath);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR creating metadata with path " + filepath));
				}
			}
			int mdupdated = 0;
			for (String md : metadataToUpdate) {
				String filepath = DATA_PREFIX + version + "/metadata" + md;
				BagFile file = bag.getBagFile(filepath);
				String filehash = bag.getChecksums(filepath).get(Algorithm.SHA1);
				boolean skipupdate = false;
				if ( previousVersion != null ) {
					String oldfilepath = DATA_PREFIX + previousVersion + "/metadata" + md;
					BagFile oldfile = bag.getBagFile(oldfilepath);
					String oldfilehash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
					if ( oldfilehash != null && oldfile.getSize() == file.getSize() && oldfilehash.equals(filehash) ) {
						skipupdate = true;
					}
				} 
				if ( !skipupdate ) {
					try {
						logger.log(Level.INFO, "  - update metadata: " + filepath);
						updateMetadata(wskey, bag.getBagFile(filepath), md, globalparams);
						mdupdated++;
					} catch ( InvalidPathException e ) {
						logger.log(Level.SEVERE, "  - METADATA UPDATE ERROR: " + filepath);
						throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR updating metadata with path " + filepath));
					}
				}
			}
			int mddeleted = 0;
			for (String md : metadataToDelete) {
				String filepath = DATA_PREFIX + version + "/metadata" + md;
				try {
					logger.log(Level.INFO, "  - delete metadata: " + filepath);
					deleteMetadata(wskey, md);
					mddeleted++;
				} catch ( InvalidPathException e ) {
					logger.log(Level.SEVERE, "  - METADATA DELETE ERROR: " + filepath);
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - ERROR deleting metadata with path " + filepath));
				}
			}
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), " - metadata summary: " + mdcreated + " created, " + mdupdated + " updated, " + mddeleted + " deleted"));
			
			if (!version.equals(Workspace.HEAD)) {
				logger.log(Level.FINE, "- snapshot version " + version);
				snapshotWorkspace(wskey, version.substring(version.lastIndexOf("/")));
				
				//logger.log(Level.FINE, "- publish version " + version);
				//publishVersion(root);
			}
			
			previousVersion = version;
			previousObjects = objects;
			previousMetadata = metadata;
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Version: " + version + " imported."));
			
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

	private String createWorkspace(Bag bag) throws RuntimeEngineTaskException {
		String wskey = UUID.randomUUID().toString();
		BagFile propFile = bag.getBagFile("data/workspace.properties");
		if (propFile == null || !propFile.exists()) {
			throw new RuntimeEngineTaskException("Workspace properties file does not exists, create one !!");
		}
		try {
			Properties props = new Properties();
			props.load(propFile.newInputStream());
			if (props.getProperty("alias") != null) {
				getCoreService().createWorkspace(wskey, props.getProperty("alias"), props.getProperty("name"), props.getProperty("type"));
			} else {
				getCoreService().createWorkspace(wskey, props.getProperty("name"), props.getProperty("type"));
			}
		} catch (Exception e2) {
			logger.log(Level.SEVERE, "unable to create workspace", e2);
			throw new RuntimeEngineTaskException("unable to create workspace", e2);
		}
		return wskey;
	}
	
	private void loadWorkspaceParams(String wskey, Map<String, Object> params) throws RuntimeEngineTaskException {
		try {
			Workspace ws = getCoreService().readWorkspace(wskey);
			params.put("workspace.key", wskey);
			params.put("workspace.members", ws.getMembers());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to read workspace", e);
			throw new RuntimeEngineTaskException("unable to read workspace", e);
		}
	}

	private List<String> listVersions(Bag bag) {
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

	private void createObject(String wskey, BagFile file, String sha1, String path, Set<String> cache) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			if ( sha1 == null || (sha1 != null && !getBinaryStore().contains(sha1)) ) {
				InputStream is = file.newInputStream();
				sha1 = getCoreService().put(is);
				is.close();
			}
			PathBuilder opath = PathBuilder.fromPath(path);
			PathBuilder oppath = opath.clone().parent();
			if (!oppath.isRoot() && !cache.contains(oppath.build())) {
				String[] parents = opath.clone().parent().buildParts();
				String current = "";
				for (int i = 0; i < parents.length; i++) {
					current += "/" + parents[i];
					if (!cache.contains(current)) {
						try {
							getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
						} catch (InvalidPathException e) {
							getCoreService().createCollection(wskey, current, "");
						}
						cache.add(current);
					}
				}
			}
			String current = opath.build();
			getCoreService().createDataObject(wskey, current, "", sha1);
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close input stream", e);
		} catch (BinaryStoreServiceException | CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error creating object for path [" + path + "]", e);
		} 
	}
	
	private void updateObject(String wskey, BagFile file, String path) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			InputStream is = file.newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().updateDataObject(wskey, path, "", hash);
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error updating object for path [" + path + "]", e);
		}
	}

	private void deleteObject(String wskey, String path) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().deleteDataObject(wskey, path);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			logger.log(Level.FINE, "Error deleting object for path: " + path, e);
			throw new RuntimeEngineTaskException("Error creating object for path [" + path + "]", e);
		}

		PathBuilder opath = PathBuilder.fromPath(path).parent();
		while (!opath.isRoot()) {
			try {
				getCoreService().deleteCollection(wskey, opath.build());
			} catch (CollectionNotEmptyException | CoreServiceException | KeyNotFoundException | AccessDeniedException e) {
				break;
			}
			opath.parent();
		}
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
	
	private String[] parseMetadataName(String path) {
		String[] mdname = new String[3];
		int lastPathIndex = path.lastIndexOf("/");
		mdname[0] = path;
		mdname[1] = "unknown";
		mdname[2] = "/";
		if (lastPathIndex > -1) {
			mdname[0] = path.substring(lastPathIndex + 1);
			mdname[2] = path.substring(0, lastPathIndex);
		}
		if (mdname[0].indexOf("[") == 0 && mdname[0].indexOf("]") >= 0) {
			mdname[1] = mdname[0].substring(1, mdname[0].indexOf("]")).trim();
			mdname[0] = mdname[0].substring(mdname[0].indexOf("]") + 1).trim();
		}
		return mdname;
	}

	private void createMetadata(String wskey, BagFile file, String path, Map<String, Object> params) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			InputStream is = file.newInputStream();
			StringWriter writer = new StringWriter();
			TemplateEngine.evaluate(params, writer, "import-metadata", new InputStreamReader(is));
			String sha1 = getCoreService().put(new ByteArrayInputStream(writer.toString().getBytes()));
			is.close();
			String[] mdname = parseMetadataName(path);
			getCoreService().createMetadataObject(wskey, mdname[2], mdname[0], mdname[1], sha1);
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error creating metadata for path [" + path + "]", e);
		} 
	}
	
	private void updateMetadata(String wskey, BagFile file, String path, Map<String, Object> params) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			InputStream is = file.newInputStream();
			StringWriter writer = new StringWriter();
			TemplateEngine.evaluate(params, writer, "import-metadata", new InputStreamReader(is));
			String sha1 = getCoreService().put(new ByteArrayInputStream(writer.toString().getBytes()));
			is.close();
			String[] mdname = parseMetadataName(path);
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, mdname[2]);
			getCoreService().updateMetadataObject(wskey, mdname[2], mdname[0], mdname[1], sha1);
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error updating metadata for path [" + path + "]", e);
		} 
	}
	
	private void deleteMetadata(String wskey, String path) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			String[] mdname = parseMetadataName(path);
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, mdname[2]);
			getCoreService().deleteMetadataObject(wskey, mdname[2], mdname[0]);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error deleting metadata for path [" + path + "]", e);
		} 
	}
	
	private void snapshotWorkspace(String wskey, String name) throws RuntimeEngineTaskException {
		try {
			getCoreService().snapshotWorkspace(wskey, name);
		} catch (CoreServiceException | KeyNotFoundException | AccessDeniedException | RuntimeEngineTaskException e) {
			logger.log(Level.FINE, "Error during workspace snapshot", e);
			throw new RuntimeEngineTaskException("Error during workspace snapshot", e);
		}
	}

}