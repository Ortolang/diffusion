package fr.ortolang.diffusion.store.binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStreamFactory;
import fr.ortolang.diffusion.store.binary.hash.SHA1FilterInputStreamFactory;

/**
 * Local FileSystem based implementation of the BinaryStoreService.<br/>
 * <br/>
 * This implementation store all contents in the provided base folder in the local file system using a SHA1 hash generator.
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@Local(BinaryStoreService.class)
@Singleton(name = BinaryStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({"system", "user"})
@Lock(LockType.READ)
public class BinaryStoreServiceBean implements BinaryStoreService {

	public static final String DEFAULT_BINARY_HOME = "binary-store";
	public static final int DISTINGUISH_SIZE = 2;
	
	private static final Logger logger = Logger.getLogger(BinaryStoreServiceBean.class.getName());

	private HashedFilterInputStreamFactory factory;
	private Path base;
	private Path working;
	private Path collide;
	
	public BinaryStoreServiceBean() {
	}
	
	@PostConstruct
	public void init() {
		this.base = Paths.get(OrtolangConfig.getInstance().getProperty("home"), DEFAULT_BINARY_HOME);
		this.working = Paths.get(base.toString(), "work");
		this.collide = Paths.get(base.toString(), "collide");
		this.factory = new SHA1FilterInputStreamFactory();
		logger.log(Level.FINEST, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
			Files.createDirectories(working);
			Files.createDirectories(collide);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to initialize binary store", e);
		}
	}
	
	public HashedFilterInputStreamFactory getHashedFilterInputStreamFactory() {
		return factory;
	}

	public void setHashedFilterInputStreamFactory(HashedFilterInputStreamFactory factory) {
		this.factory = factory;
	}

	public Path getBase() {
		return base;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean contains(String identifier) throws BinaryStoreServiceException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
		if (Files.exists(path)) {
			return true;
		}
		return false;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream get(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
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
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public File getFile(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			return path.toFile();
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long size(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			return Files.size(path);
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String type(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			Tika tika = new Tika();
			return tika.detect(path.toFile()); 
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String extract(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
		if (!Files.exists(path)) {
			throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			Tika tika = new Tika();
			tika.setMaxStringLength(20000000);
			String result = tika.parseToString(path.toFile());
			return  result;
		} catch (Exception e) {
			throw new BinaryStoreServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String put(InputStream content) throws BinaryStoreServiceException, DataCollisionException {
		try {
			HashedFilterInputStream input = factory.getHashedFilterInputStream(content);
			try {
				Path tmpfile = Paths.get(working.toString(), Long.toString(System.nanoTime()));
				
				Files.copy(input, tmpfile);
				logger.log(Level.FINE, "content stored in local temporary file: " + tmpfile.toString());
				String hash = input.getHash();
				logger.log(Level.FINE, "content based generated sha1 hash: " + hash);
				
				Path parent = Paths.get(base.toString(), hash.substring(0, DISTINGUISH_SIZE));
				Path file = Paths.get(base.toString(), hash.substring(0, DISTINGUISH_SIZE), hash);
				if (!Files.exists(parent)) {
					Files.createDirectory(parent);
				}
				if (!Files.exists(file)) {
					Files.move(tmpfile, file);
					logger.log(Level.FINE, "content moved in local definitive file: " + file.toString());
				} else {
					logger.log(Level.INFO, "a file with same hash already exists, trying to detect collision");
					try (InputStream input1 = Files.newInputStream(file); 
							InputStream input2 = Files.newInputStream(tmpfile)) {
						if ( IOUtils.contentEquals(input1, input2) ) {
							Files.delete(tmpfile);
						} else {
							logger.log(Level.SEVERE, "BINARY COLLISION DETECTED - storing colliding files in dedicated folder");
							Files.copy(file, Paths.get(collide.toString(), hash + ".origin"));
							Files.move(tmpfile, Paths.get(collide.toString(), hash + ".colliding"));
							throw new DataCollisionException();
						}
					} 
				}
				return hash;
			} catch ( IOException e) {
				throw new BinaryStoreServiceException(e);
			} finally {
				IOUtils.closeQuietly(input);
			}
		} catch ( NoSuchAlgorithmException e ) {
			throw new BinaryStoreServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void check(String identifier) throws BinaryStoreServiceException, DataNotFoundException, DataCorruptedException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
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
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void delete(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, DISTINGUISH_SIZE), identifier);
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
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String generate(InputStream content) throws BinaryStoreServiceException {
		try {
			HashedFilterInputStream input = factory.getHashedFilterInputStream(content);
			byte[] buffer = new byte[10240];
			while ( input.read(buffer) >= 0) {
			}
			return input.getHash();
		} catch (Exception e) {
			throw new BinaryStoreServiceException("Unable to generate a hash for this content: " + e.getMessage(), e);
		}
	}

}