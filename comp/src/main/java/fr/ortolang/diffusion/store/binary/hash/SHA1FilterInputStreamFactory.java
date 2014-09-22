package fr.ortolang.diffusion.store.binary.hash;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public class SHA1FilterInputStreamFactory implements HashedFilterInputStreamFactory {

	@Override
	public HashedFilterInputStream getHashedFilterInputStream(InputStream in) throws NoSuchAlgorithmException {
		return new SHA1FilterInputStream(in);
	}

}
