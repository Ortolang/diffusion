package fr.ortolang.diffusion.store.binary.hash;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public interface HashedFilterInputStreamFactory {
	
	public HashedFilterInputStream getHashedFilterInputStream(InputStream is) throws NoSuchAlgorithmException;

}
