package fr.ortolang.diffusion.core;

import java.io.InputStream;

import fr.ortolang.diffusion.BinaryDataSource;
import fr.ortolang.diffusion.DiffusionService;
import fr.ortolang.diffusion.core.entity.ObjectContainer;
import fr.ortolang.diffusion.registry.KeyAlreadyBoundException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

public interface CoreService extends DiffusionService, BinaryDataSource {
	
	public static final String SERVICE_NAME = "Core";
	public static final String[] OBJECT_TYPE_LIST = new String[] { ObjectContainer.OBJECT_TYPE };
	
	public void createContainer(String key, String name) throws CoreServiceException, KeyAlreadyBoundException;
	
	public ObjectContainer getContainer(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void deleteContainer(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void addDataStreamToContainer(String key, String name, InputStream data) throws CoreServiceException, KeyNotFoundException;
	
	public void removeDataStreamFromContainer(String key, String name)  throws CoreServiceException, KeyNotFoundException;
	
	public InputStream getDataStreamFromContainer(String key, String name) throws CoreServiceException, KeyNotFoundException;
	
}
