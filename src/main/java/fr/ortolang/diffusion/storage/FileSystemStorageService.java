package fr.ortolang.diffusion.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

public class FileSystemStorageService implements StorageService {

	private static final Logger logger = Logger.getLogger(FileSystemStorageService.class.getName());

	private StorageIdentifierGenerator generator;
	private Path base;

	public FileSystemStorageService(Path base) throws StorageServiceException {
		generator = new SHA1IdentifierGenerator();
		this.base = base;
		init();
	}

	private void init() throws StorageServiceException {
		logger.log(Level.FINEST, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
		} catch (Exception e) {
			throw new StorageServiceException("Error during initialization: " + e.getMessage(), e);
		}
	}

	public void setStorageIdentifierGenerator(StorageIdentifierGenerator generator) {
		this.generator = generator;
	}

	public StorageIdentifierGenerator getStorageIdentifierGenerator() {
		return generator;
	}
	
	public Path getBase() {
		return base;
	}

	@Override
	public InputStream get(String identifier) throws StorageServiceException, ObjectNotFoundException {
		// TODO Maybe check corruption and generate a ObjectCorruptedException !!
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			return Files.newInputStream(path);
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public String put(InputStream content) throws StorageServiceException, ObjectAlreadyExistsException, ObjectCollisionException {
		try {
			String identifier = generator.generate(content);
			Path parent = Paths.get(base.toString(), identifier.substring(0, 4));
			Path file = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
			if (!Files.exists(parent)) {
				Files.createDirectory(parent);
			}
			if (!Files.exists(file)) {
				Files.copy(content, file);
			} else {
				try (InputStream input = Files.newInputStream(file)) {
					if ( IOUtils.contentEquals(content, input) ) {
						throw new ObjectAlreadyExistsException();
					} else {
						throw new ObjectCollisionException();
					}
				} 
			}
			return identifier;
		} catch (IOException e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public void check(String identifier) throws StorageServiceException, ObjectNotFoundException, ObjectCorruptedException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		String check;
		try {
			check = generator.generate(Files.newInputStream(path));
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
		if (!check.equals(identifier)) {
			throw new ObjectCorruptedException("The object with id [" + identifier
					+ "] is CORRUPTED. The stored object's content has generate a wrong identifier [" + check + "]");
		}
	}

	@Override
	public void delete(String identifier) throws StorageServiceException, ObjectNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public String generate(InputStream content) throws StorageServiceException {
		try {
			String hash = generator.generate(content);
			return hash;
		} catch (Exception e) {
			throw new StorageServiceException("Unable to generate an identifier for this content: " + e.getMessage(), e);
		}
	}

}
