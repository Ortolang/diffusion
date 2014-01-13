package fr.ortolang.diffusion.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	@Override
	public DigitalObject get(String identifier) throws StorageServiceException, ObjectNotFoundException {
		//TODO Maybe check corruption and generate a ObjectCorruptedException !!
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an DigitalObject with id: " + identifier + " in the storage");
		}
		try {
			DigitalObject object = new DigitalObject(Files.newInputStream(path));
			return object;
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public String put(DigitalObject object) throws StorageServiceException, ObjectAlreadyExistsException {
		try {
			String identifier = generator.generate(object.getData());
			Path parent = Paths.get(base.toString(), identifier.substring(0, 4));
			Path file = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
			if (!Files.exists(parent)) {
				Files.createDirectory(parent);
			}
			if (!Files.exists(file)) {
				Files.copy(object.getData(), file);
			} else {
				// TODO Check if it is a collision.
			}
			return identifier;
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public void verify(String identifier) throws StorageServiceException, ObjectNotFoundException, ObjectCorruptedException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an DigitalObject with id: " + identifier + " in the storage");
		}
		String check;
		try {
			check = generator.generate(Files.newInputStream(path));
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
		if (!check.equals(identifier)) {
			throw new ObjectCorruptedException("The DigitalObject with id: " + identifier
					+ " is CORRUPTED. The stored content has generate a wrong hash: " + check);
		}
	}

	@Override
	public void delete(String identifier) throws StorageServiceException, ObjectNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an DigitalObject with id: " + identifier + " in the storage");
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			throw new StorageServiceException(e);
		}
	}

	@Override
	public String hash(DigitalObject object) throws StorageServiceException {
		try (InputStream input = object.getData()) {
			String hash = generator.generate(input);
			return hash;
		} catch (Exception e) {
			throw new StorageServiceException("Unable to generate StorageIdentifier for DigitalObject: " + e.getMessage(), e);
		}
	}

}
