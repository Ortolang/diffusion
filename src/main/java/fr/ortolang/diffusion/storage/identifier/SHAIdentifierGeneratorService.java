package fr.ortolang.diffusion.storage.identifier;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * SHA1 implementation of the IdentifierGeneratorService
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public class SHAIdentifierGeneratorService implements IdentifierGeneratorService {
	
	public SHAIdentifierGeneratorService() {
	}

	@Override
	public String generate(InputStream input) throws IdentifierGeneratorException {
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
			throw new IdentifierGeneratorException(e);
		}
	}

}
