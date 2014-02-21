package fr.ortolang.diffusion.core;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteOutputStream;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.entity.DigitalCollection;
import fr.ortolang.diffusion.core.entity.DigitalObject;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.registry.BranchNotAllowedException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

public interface CoreService extends OrtolangService, OrtolangBinaryService {
	
	public static final String SERVICE_NAME = "core";
	public static final String[] OBJECT_TYPE_LIST = new String[] { DigitalObject.OBJECT_TYPE, DigitalCollection.OBJECT_TYPE, DigitalReference.OBJECT_TYPE };
	
	public void createObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyAlreadyExistsException;
	
	public void createObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyAlreadyExistsException;
	
	public DigitalObject getObject(String key) throws CoreServiceException, KeyNotFoundException;
	
	public byte[] getObjectData(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void getObjectData(String key, RemoteOutputStream ros) throws CoreServiceException, KeyNotFoundException;
	
	public void updateObject(String key, String name, String description) throws CoreServiceException, KeyNotFoundException;
	
	public void updateObject(String key, String name, String description, byte[] data) throws CoreServiceException, KeyNotFoundException;
	
	public void updateObject(String key, String name, String description, RemoteInputStream data) throws CoreServiceException, KeyNotFoundException;
	
	public void cloneObject(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException;
	
	public void deleteObject(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void createCollection(String key, String name, String description) throws CoreServiceException, KeyAlreadyExistsException;
	
	public DigitalCollection getCollection(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void updateCollection(String key, String name, String description) throws CoreServiceException, KeyNotFoundException;
	
	public void addElementToCollection(String key, String element) throws CoreServiceException, KeyNotFoundException;
	
	public void removeElementFromCollection(String key, String element) throws CoreServiceException, KeyNotFoundException;
	
	public void cloneCollection(String key, String origin) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException;
	
	public void deleteCollection(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void createReference(String key, boolean dynamic, String name, String target) throws CoreServiceException, KeyAlreadyExistsException;
	
	public DigitalReference getReference(String key) throws CoreServiceException, KeyNotFoundException;
	
}
