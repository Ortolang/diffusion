package fr.ortolang.diffusion.core;

import java.io.InputStream;
import java.io.OutputStream;

import fr.ortolang.diffusion.registry.EntryNotFoundException;

public interface CoreServiceLocal {
	
	public void addDataStreamToContainer(String key, String name, InputStream data) throws CoreServiceException, EntryNotFoundException;
	
	public void getDataStreamFromContainer(String key, String name, OutputStream os) throws CoreServiceException, EntryNotFoundException;

}
