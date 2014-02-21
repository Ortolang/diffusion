package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.io.OutputStream;

import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

public interface CoreServiceLocal {
	
	public void createObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyAlreadyExistsException;
	
	public void updateObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyNotFoundException;
	
	public void getObjectData(String key, OutputStream os) throws CoreServiceException, KeyNotFoundException;
	
}
