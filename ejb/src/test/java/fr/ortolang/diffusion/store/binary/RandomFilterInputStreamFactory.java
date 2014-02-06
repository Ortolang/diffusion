package fr.ortolang.diffusion.store.binary;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStreamFactory;

public class RandomFilterInputStreamFactory implements HashedFilterInputStreamFactory {

	@Override
	public HashedFilterInputStream getHashedFilterInputStream(InputStream is) throws NoSuchAlgorithmException {
		return new RandomFilterInputStream(is);
	}

}