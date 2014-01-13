package fr.ortolang.diffusion.storage;

import java.io.InputStream;

public interface StorageIdentifierGenerator {
	
	public String generate(InputStream Stream) throws StorageServiceException;

}
