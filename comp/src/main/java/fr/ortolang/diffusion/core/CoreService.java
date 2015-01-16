package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.util.List;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public interface CoreService extends OrtolangService, OrtolangBinaryService, OrtolangIndexableService {
	
	public static final String SERVICE_NAME = "core";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Workspace.OBJECT_TYPE, DataObject.OBJECT_TYPE, Collection.OBJECT_TYPE, Link.OBJECT_TYPE, MetadataObject.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Workspace.OBJECT_TYPE, "read,update,delete,snapshot" },
		{ DataObject.OBJECT_TYPE, "read,update,delete,download" },
		{ Collection.OBJECT_TYPE, "read,update,delete,download" },
		{ Link.OBJECT_TYPE, "read,update,delete,download" },
		{ MetadataObject.OBJECT_TYPE, "read,update,delete,download"}};
	
	/* Workspace */
	
	public void createWorkspace(String workspace, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public Workspace readWorkspace(String workspace) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void updateWorkspace(String workspace, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void snapshotWorkspace(String workspace, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteWorkspace(String workspace) throws CoreServiceException, KeyNotFoundException, KeyNotFoundException, AccessDeniedException;

	public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public String resolveWorkspacePath(String workspace, String root, String path) throws CoreServiceException, InvalidPathException, AccessDeniedException;
	
	public String resolveWorkspaceMetadata(String workspace, String root, String path, String name) throws CoreServiceException, InvalidPathException, AccessDeniedException;
	
	/*Collection*/
	
	public void createCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void moveCollection(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteCollection(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;

	public String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, InvalidPathException;
	
	/*DataObject*/
	
	public void createDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void moveDataObject(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteDataObject(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	/*Link*/
	
	public void createLink(String workspace, String path, String target) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, AccessDeniedException;
	
	public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException;
	
	public void moveLink(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteLink(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*MetadataObject*/
	
	public void createMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteMetadataObject(String workspace, String path, String name) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	/*BinaryContent*/
	
	public InputStream download(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;
	
	public InputStream preview(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;
	
	public String put(InputStream data) throws CoreServiceException, DataCollisionException;
	
}
