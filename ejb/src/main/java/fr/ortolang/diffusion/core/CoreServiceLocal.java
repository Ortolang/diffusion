package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.io.OutputStream;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface CoreServiceLocal extends OrtolangIndexableService {
	
	public void createDataObject(String key, String name, String description, InputStream data) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void updateDataObjectContent(String key, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void readDataObjectContent(String key, OutputStream os) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void createMetadataObject(String key, String name, InputStream data, String target) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void updateMetadataObjectContent(String key, InputStream data) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void readMetadataObjectContent(String key, OutputStream os) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
}
