package fr.ortolang.diffusion.core;

import java.util.Set;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteOutputStream;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface CoreService extends OrtolangService, OrtolangBinaryService {
	
	public static final String SERVICE_NAME = "core";
	public static final String[] OBJECT_TYPE_LIST = new String[] { DataObject.OBJECT_TYPE, Collection.OBJECT_TYPE, Link.OBJECT_TYPE, MetadataObject.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ DataObject.OBJECT_TYPE, "read,update,delete" },
		{ Collection.OBJECT_TYPE, "read,update,delete" },
		{ Link.OBJECT_TYPE, "read,update,delete" },
		{ MetadataObject.OBJECT_TYPE, "read,update,delete"}};
	
	/*Collection*/
	
	public void createCollection(String key, String name, String description) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateCollection(String key, String name, String description) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void addElementToCollection(String key, String element, boolean inheritSecurity) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void removeElementFromCollection(String key, String element) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void cloneCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void cloneCollectionContent(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public Set<String> listCollectionContent(String key) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void forkCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*DataObject*/
	
	public void createDataObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void createDataObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public byte[] readDataObjectContent(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void readDataObjectContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateDataObject(String key, String name, String description) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateDataObjectContent(String key, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateDataObjectContent(String key, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void cloneDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void forkDataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*MetadataObject*/
	
	public void createMetadataObject(String key, String name, byte[] data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void createMetadataObject(String key, String name, RemoteInputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public byte[] readMetadataObjectContent(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void readMetadataObjectContent(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateMetadataObject(String key, String name, String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateMetadataObjectContent(String key, byte[] data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateMetadataObjectContent(String key, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void cloneMetadataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void forkMetadataObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*Link*/
	
	public void createLink(String key, String name, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateLink(String key, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void cloneLink(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void forkLink(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
}
