package fr.ortolang.diffusion.runtime.engine.task;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
import fr.ortolang.diffusion.core.PathAlreadyExistsException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;
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

public class ImportContentTask extends RuntimeEngineTask {

	private static final Logger LOGGER = Logger.getLogger(ImportContentTask.class.getName());

	public static final String NAME = "Import Content";
	private Set<String> collectionCreationCache = new HashSet<String>();
	private String wskey;
	
	public ImportContentTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
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
		StringBuilder report = new StringBuilder();
		LOGGER.log(Level.FINE, "purge collection creation cache.");
		purgeCache();
        try {
			String line = null;
			boolean needcommit;
			long tscommit = System.currentTimeMillis();
			while ((line = reader.readLine()) != null) {
				LOGGER.log(Level.FINE, "- executing operation: " + line);
				needcommit = false;
				String[] operation = line.split("\t", -1);
				try {
					switch (operation[0]) {
						case "create-workspace":
							wskey = UUID.randomUUID().toString();
							createWorkspace(operation[1], operation[2], operation[3], operation[4], operation[5]);
							execution.setVariable(WORKSPACE_KEY_PARAM_NAME, wskey);
							needcommit = true;
							report.append("[DONE] " + line + "\r\n");
							break;
						case "create-object":
							createObject(bag, operation[1], operation[2], operation[3]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "update-object":
							updateObject(bag, operation[1], operation[2]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "delete-object":
							deleteObject(operation[2]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "create-metadata":
							createMetadata(bag, operation[1], operation[2], operation[3]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "update-metadata":
							updateMetadata(bag, operation[1], operation[2], operation[3]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "delete-metadata":
							deleteMetadata(operation[2], operation[3]);
							report.append("[DONE] " + line + "\r\n");
							break;
						case "snapshot-workspace":
							snapshotWorkspace(operation[1]);
							purgeCache();
							needcommit = true;
							report.append("[DONE] " + line + "\r\n");
							break;
						default:
							partial = true;
							report.append("[ERROR] " + line + " \r\n\t -> Unknown operation\r\n");
					}
				} catch ( Exception e ) {
					partial = true;
					report.append("[ERROR] " + line + " \r\n\t -> Message: " + e.getMessage() + "\r\n");
					LOGGER.log(Level.FINE, "ImportContentTask exception raised", e);
				}
				if ( System.currentTimeMillis() - tscommit > 30000 ) {
					LOGGER.log(Level.FINE, "current transaction exceed 30sec, need commit.");
					needcommit = true;
				}
				try {
					if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
						LOGGER.log(Level.FINE, "committing active user transaction.");
						getUserTransaction().commit();
						tscommit = System.currentTimeMillis();
						getUserTransaction().begin();
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
				}
				if ( partial ) {
				    report.append("[ERROR] Stopping content import due to previous errors... \r\n");
				    break;
				}
			}
		} catch (IOException e) {
			partial = true;
			report.append("[ERROR] unable to read script \r\n\t -> Message: " + e.getMessage() + "\r\n");
			LOGGER.log(Level.SEVERE, "- unexpected error during reading operations script", e);
		}
		try {
			LOGGER.log(Level.FINE, "committing active user transaction and starting new one.");
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
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), report.toString(), null));
		if ( partial ) {
		    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some content has not been imported (see trace for detail), import aborted !!"));
		    throw new RuntimeEngineTaskException("Unable to fullfill the import content task due to errors");
		} else {
		    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All content imported successfully"));
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
			    getSecurityService().changeOwner(ws.getEventFeed(), owner);
				getSecurityService().changeOwner(wskey, owner);
				getMembershipService().addMemberInGroup(ws.getMembers(), owner);
                getMembershipService().removeMemberFromGroup(ws.getMembers(), MembershipService.SUPERUSER_IDENTIFIER);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to create workspace", e);
			throw new RuntimeEngineTaskException("unable to create workspace : " + e.getMessage(), e);
		}
	}

	private void createObject(Bag bag, String bagpath, String sha1, String path) throws RuntimeEngineTaskException {
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
						} catch (PathNotFoundException e) {
							getCoreService().createCollection(wskey, current);
						}
						collectionCreationCache.add(current);
					}
				}
			}
			String current = opath.build();
			getCoreService().createDataObject(wskey, current, sha1);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (BinaryStoreServiceException | CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException | PathAlreadyExistsException e) {
			throw new RuntimeEngineTaskException("Error creating object for path [" + path + "] : " + e.getMessage(), e);
		} 
	}

	private void updateObject(Bag bag, String bagpath, String path) throws RuntimeEngineTaskException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().updateDataObject(wskey, path, hash);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException e) {
			throw new RuntimeEngineTaskException("Error updating object for path [" + path + "] : " + e.getMessage(), e);
		}
	}

	private void deleteObject(String path) throws RuntimeEngineTaskException {
		try {
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().deleteDataObject(wskey, path);
			PathBuilder opath = PathBuilder.fromPath(path).parent();
	        while (!opath.isRoot()) {
	            try {
	                getCoreService().deleteCollection(wskey, opath.build());
	            } catch (CollectionNotEmptyException | CoreServiceException | KeyNotFoundException | AccessDeniedException | WorkspaceReadOnlyException e) {
	                break;
	            }
	            opath.parent();
	        }
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException e ) {
			LOGGER.log(Level.FINE, "Error deleting object for path: " + path, e);
			throw new RuntimeEngineTaskException("Error deleting object for path [" + path + "] : " + e.getMessage(), e);
		}
	}

	private void createMetadata(Bag bag, String bagpath, String path, String name) throws RuntimeEngineTaskException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().createMetadataObject(wskey, path, name, hash, null, false);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException | MetadataFormatException e ) {
			throw new RuntimeEngineTaskException("Error creating metadata for path [" + path + "] : " + e.getMessage(), e);
		} 
	}

	private void updateMetadata(Bag bag, String bagpath, String path, String name) throws RuntimeEngineTaskException {
		try {
			InputStream is = bag.getBagFile(bagpath).newInputStream();
			String hash = getCoreService().put(is);
			is.close();
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().updateMetadataObject(wskey, path, name, hash, null, false);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unable to close input stream", e);
		} catch (CoreServiceException | DataCollisionException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException | MetadataFormatException e) {
			throw new RuntimeEngineTaskException("Error updating metadata for path [" + path + "] : " + e.getMessage(), e);
		} 
	}

	private void deleteMetadata(String path, String name) throws RuntimeEngineTaskException {
		try {
			getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, path);
			getCoreService().deleteMetadataObject(wskey, path, name, false);
		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException e) {
			throw new RuntimeEngineTaskException("Error deleting metadata for path [" + path + "] : " + e.getMessage(), e);
		} 
	}

	private void snapshotWorkspace(String name) throws RuntimeEngineTaskException {
		try {
			getCoreService().snapshotWorkspace(wskey);
		} catch ( CoreServiceException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException e) {
			throw new RuntimeEngineTaskException("Error snapshoting workspace : " + e.getMessage(), e);
		} 
	}

}
