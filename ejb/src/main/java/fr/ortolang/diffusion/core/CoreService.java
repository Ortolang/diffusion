package fr.ortolang.diffusion.core;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteOutputStream;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.entity.ObjectContainer;
import fr.ortolang.diffusion.registry.EntryAlreadyExistsException;
import fr.ortolang.diffusion.registry.EntryNotFoundException;

public interface CoreService extends OrtolangService, OrtolangBinaryService {
	
	public static final String SERVICE_NAME = "Core";
	public static final String[] OBJECT_TYPE_LIST = new String[] { ObjectContainer.OBJECT_TYPE };
	
	public void createContainer(String key, String name) throws CoreServiceException, EntryAlreadyExistsException;
	
	public ObjectContainer getContainer(String key) throws CoreServiceException, EntryNotFoundException;
	
	public void deleteContainer(String key) throws CoreServiceException, EntryNotFoundException;
	
	public void addDataStreamToContainer(String key, String name, byte[] data) throws CoreServiceException, EntryNotFoundException;
	
	public void addDataStreamToContainer(String key, String name, RemoteInputStream data) throws CoreServiceException, EntryNotFoundException;
	
	public void removeDataStreamFromContainer(String key, String name)  throws CoreServiceException, EntryNotFoundException;
	
	public void getDataStreamFromContainer(String key, String name, RemoteOutputStream ros) throws CoreServiceException, EntryNotFoundException;
	
	public byte[] getDataStreamFromContainer(String key, String name) throws CoreServiceException, EntryNotFoundException;
	
}
