package fr.ortolang.diffusion.storage;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class SHA1IdentifierGenerator implements StorageIdentifierGenerator {
	
	public SHA1IdentifierGenerator() {
	}

	@Override
	public String generate(InputStream input) throws StorageServiceException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				md.update(buffer, 0, nbread);
			}
			BigInteger bi = new BigInteger(1, md.digest());
			return bi.toString(16);
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

}
