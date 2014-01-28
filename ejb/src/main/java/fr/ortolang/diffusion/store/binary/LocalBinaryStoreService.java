package fr.ortolang.diffusion.store.binary;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * Local FileSystem based implementation of the BinaryStoreService.<br/>
 * <br/>
 * This implementation store all contents in the provided base folder in the local file system using a SHA1 hash generator.
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public class LocalBinaryStoreService implements BinaryStoreService {

	private static final Logger logger = Logger.getLogger(LocalBinaryStoreService.class.getName());

	private Path base;
	private Path working;
	
	public LocalBinaryStoreService(Path base) throws BinaryStoreServiceException {
		this.base = base;
		this.working = Paths.get(base.toString(), "work");
		init();
	}

	private void init() throws BinaryStoreServiceException {
		logger.log(Level.FINEST, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
			Files.createDirectories(working);
		} catch (Exception e) {
			throw new BinaryStoreServiceException("Error during initialization: " + e.getMessage(), e);
		}
	}

	public Path getBase() {
		return base;
	}

	@Override
	public InputStream get(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			return Files.newInputStream(path);
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}

	@Override
	public String put(InputStream content) throws BinaryStoreServiceException, DataCollisionException {
		try {
			SHA1FilteredInputStream input = new SHA1FilteredInputStream(content);
			Path tmpfile = Paths.get(working.toString(), Long.toString(System.currentTimeMillis()));
			
			Files.copy(input, tmpfile);
			logger.log(Level.INFO, "content stored in local temporary file: " + tmpfile.toString());
			String hash = input.getHashString();
			logger.log(Level.INFO, "content based generated sha1 hash: " + hash);
			
			Path parent = Paths.get(base.toString(), hash.substring(0, 4));
			Path file = Paths.get(base.toString(), hash.substring(0, 4), hash);
			if (!Files.exists(parent)) {
				Files.createDirectory(parent);
			}
			if (!Files.exists(file)) {
				Files.move(tmpfile, file);
				logger.log(Level.INFO, "content moved in local definitive file: " + file.toString());
			} else {
				logger.log(Level.INFO, "a file with same hash already exists, trying to detect collision");
				try (InputStream input1 = Files.newInputStream(file); 
						InputStream input2 = Files.newInputStream(tmpfile)) {
					if ( !IOUtils.contentEquals(input1, input2) ) {
						throw new DataCollisionException();
					} else {
						Files.delete(tmpfile);
					}
				} 
			}
			return hash;
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new BinaryStoreServiceException(e);
		} 
	}

	@Override
	public void check(String identifier) throws BinaryStoreServiceException, DataNotFoundException, DataCorruptedException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		String check;
		try (InputStream input = Files.newInputStream(path)) {
			check = generate(input);
		} catch (IOException e) {
			throw new BinaryStoreServiceException(e);
		} 
		if (!check.equals(identifier)) {
			throw new DataCorruptedException("The object with id [" + identifier
					+ "] is CORRUPTED. The stored object's content has generate a wrong identifier [" + check + "]");
		}
	}

	@Override
	public void delete(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}

	@Override
	public String generate(InputStream content) throws BinaryStoreServiceException {
		try {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				byte[] buffer = new byte[10240];
				int nbread = 0;
				while ((nbread = content.read(buffer)) > 0) {
					md.update(buffer, 0, nbread);
				}
				BigInteger bi = new BigInteger(1, md.digest());
				return bi.toString(16);
			} catch (Exception e) {
				throw new BinaryStoreServiceException(e);
			}
		} catch (Exception e) {
			throw new BinaryStoreServiceException("Unable to generate an identifier for this content: " + e.getMessage(), e);
		}
	}

}
