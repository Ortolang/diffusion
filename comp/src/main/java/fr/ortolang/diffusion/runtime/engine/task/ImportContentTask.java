package fr.ortolang.diffusion.runtime.engine.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CollectionNotEmptyException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class ImportContentTask extends RuntimeEngineTask {

	private static final Logger LOGGER = Logger.getLogger(ImportContentTask.class.getName());

	public static final String NAME = "Import Content";
	private Set<String> collectionCreationCache = new HashSet<String>();
	private String wskey;
	
	public ImportContentTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		LOGGER.log(Level.INFO, "Starting Import Content Task");
		checkParameters(execution);
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		Bag bag = loadBag(bagpath);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag file loaded from local file: " + bag.getFile()));
		BufferedReader reader = new BufferedReader(new StringReader(execution.getVariable(IMPORT_OPERATIONS_PARAM_NAME, String.class)));
		try {
			if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
				LOGGER.log(Level.FINE, "starting new user transaction.");
				getUserTransaction().begin();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
		}
		
		boolean partial = false;
		try {
			String line = null;
			boolean needcommit;
			long tscommit = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				LOGGER.log(Level.FINE, "- executing operation: " + line);
				needcommit = false;
				String[] operation = line.split("\t");
				try {
					switch (operation[0]) {
						case "create-workspace":
							wskey = UUID.randomUUID().toString();
							createWorkspace(operation[1], operation[2], operation[3], operation[4], operation[5]);
							execution.setVariable(WORKSPACE_KEY_PARAM_NAME, wskey);
							needcommit = true;
							break;
						case "create-object":
							createObject(bag, operation[1], operation[2], operation[3]);
							break;
						case "update-object":
							updateObject(bag, operation[1], operation[2]);
							break;
						case "delete-object":
							deleteObject(operation[2]);
							break;
						case "create-metadata":
							createMetadata(bag, operation[1], operation[2], operation[3]);
							break;
						case "update-metadata":
							updateMetadata(bag, operation[1], operation[2], operation[3]);
							break;
						case "delete-metadata":
							deleteMetadata(operation[1], operation[2]);
							break;
						case "snapshot-workspace":
							snapshotWorkspace(operation[1]);
							purgeCache();
							needcommit = true;
							break;
						default:
							partial = true;
							throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unknown operation: " + line));
					}
				} catch ( Exception e ) {
					partial = true;
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error while executing operation: " + line));
				}
				if ( System.currentTimeMillis() - tscommit > 300000 ) {
					LOGGER.log(Level.FINE, "current transaction exceed 5min, need commit.");
					needcommit = true;
				}
				try {
					if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
						LOGGER.log(Level.FINE, "commiting active user transaction.");
						getUserTransaction().commit();
						tscommit = System.currentTimeMillis();
						getUserTransaction().begin();
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
				}
			}
		} catch (IOException e) {
			partial = true;
			LOGGER.log(Level.SEVERE, "- unexpected error during reading operations script", e);
		}
		try {
			LOGGER.log(Level.FINE, "commiting active user transaction.");
			getUserTransaction().commit();
			getUserTransaction().begin();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
		}
		
		try {
			bag.close();
		} catch ( IOException e ) {
			LOGGER.log(Level.SEVERE, "- error during closing bag", e);
		}
		LOGGER.log(Level.FINE, "- import content done");
		execution.setVariable("partial", partial);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import content done"));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(BAG_PATH_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		if (!execution.hasVariable(IMPORT_OPERATIONS_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + IMPORT_OPERATIONS_PARAM_NAME + " is not set");
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
			LOGGER.log(Level.WARNING, "bag verification failed: " + result.messagesToString());
			throw new RuntimeEngineTaskException("bag verification failed: " + result.messagesToString());
		}
		return bag;
	}

	private void purgeCache() {
		collectionCreationCache = new HashSet<String>();
	}
	
	private void createWorkspace(String alias, String name, String type, String owner, String members) throws RuntimeEngineTaskException {
		try {
			Workspace ws = getCoreService().createWorkspace(wskey, alias, name, type);
			if ( members != null && members.length() > 0 ) {
				for ( String member : members.split(",") ) {
					getMembershipService().addMemberInGroup(ws.getMembers(), member);
				}
			}
			if ( owner != null && owner.length() > 0 ) {
				getSecurityService().changeOwner(ws.getMembers(), owner);
				getSecurityService().changeOwner(wskey, owner);
				getMembershipService().removeMemberFromGroup(ws.getMembers(), MembershipService.SUPERUSER_IDENTIFIER);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to create workspace", e);
			throw new RuntimeEngineTaskException("unable to create workspace", e);
		}
	}

	private void createObject(Bag bag, String bagpath, String sha1, String path) throws RuntimeEngineTaskException, InvalidPathException {
		try {
			if ( sha1 == null || (sha1 != null && !getBinaryStore().contains(sha1)) ) {
				InputStream is = bag.getBagFile(bagpath).newInputStream();
				sha1 = getCoreService().put(is);
				is.close();
			}
			PathBuilder opath = PathBuilder.fromPath(path);
			PathBuilder oppath = opath.clone().parent();
			if (!oppath.isRoot() && !collectionCreationCache.contains(oppath.build())) {
				String[] parents = opath.clone().parent().buildParts();
				String current = "";
				for (int i = 0; i < parents.length; i++) {
					current += "/" + parents[i];
					if (!collectionCreationCache.contains(current)) {
						try {
							getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
						} catch (InvalidPathException e) {
							getCoreService().createCollection(wskey, current, "");
						}
						collectionCreationCache.add(current);
					}
				}
			}
			String current = opath.build();
			getCoreService().createDataObject(wskey, current, "", sha1);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (BinaryStoreServiceException | CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error creating object for path [" + path + "]", e);
		} 
	}

	private void updateObject(Bag bag, String bagpath, String path) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().updateDataObject(wskey, path, "", hash);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error updating object for path [" + path + "]", e);
		}
	}

	private void deleteObject(String path) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().deleteDataObject(wskey, path);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			LOGGER.log(Level.FINE, "Error deleting object for path: " + path, e);
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

	private void createMetadata(Bag bag, String bagpath, String path, String name) throws InvalidPathException, RuntimeEngineTaskException, MetadataFormatException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().createMetadataObject(wskey, path, name, hash);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error creating metadata for path [" + path + "]", e);
		} 
	}

	private void updateMetadata(Bag bag, String bagpath, String path, String name) throws InvalidPathException, RuntimeEngineTaskException, MetadataFormatException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().updateMetadataObject(wskey, path, name, hash);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error updating metadata for path [" + path + "]", e);
		} 
	}

	private void deleteMetadata(String path, String name) throws InvalidPathException, RuntimeEngineTaskException {
		try {
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().deleteMetadataObject(wskey, path, name);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error deleting metadata for path [" + path + "]", e);
		} 
	}

	private void snapshotWorkspace(String name) throws RuntimeEngineTaskException {
		try {
			getCoreService().snapshotWorkspace(wskey, name);
		} catch ( CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
			throw new RuntimeEngineTaskException("Error snapshoting workspace", e);
		} 
	}

}
