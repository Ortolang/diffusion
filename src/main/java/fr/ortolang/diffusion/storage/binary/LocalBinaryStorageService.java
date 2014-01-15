package fr.ortolang.diffusion.storage.binary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.storage.ObjectAlreadyExistsException;
import fr.ortolang.diffusion.storage.ObjectCollisionException;
import fr.ortolang.diffusion.storage.ObjectCorruptedException;
import fr.ortolang.diffusion.storage.ObjectNotFoundException;
import fr.ortolang.diffusion.storage.identifier.IdentifierGeneratorException;
import fr.ortolang.diffusion.storage.identifier.IdentifierGeneratorService;
import fr.ortolang.diffusion.storage.identifier.SHAIdentifierGeneratorService;

/**
 * Local FileSystem based implementation of the BinaryStorageService.<br/>
 * <br/>
 * This implementation store all contents in the provided base folder in the local filesystem.
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public class LocalBinaryStorageService implements BinaryStorageService {

	private static final Logger logger = Logger.getLogger(LocalBinaryStorageService.class.getName());

	private IdentifierGeneratorService generator;
	private Path base;

	public LocalBinaryStorageService(Path base) throws BinaryStorageException {
		generator = new SHAIdentifierGeneratorService();
		this.base = base;
		init();
	}

	private void init() throws BinaryStorageException {
		logger.log(Level.FINEST, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
		} catch (Exception e) {
			throw new BinaryStorageException("Error during initialization: " + e.getMessage(), e);
		}
	}

	public void setStorageIdentifierGenerator(IdentifierGeneratorService generator) {
		this.generator = generator;
	}

	public IdentifierGeneratorService getStorageIdentifierGenerator() {
		return generator;
	}
	
	public Path getBase() {
		return base;
	}

	@Override
	public InputStream get(String identifier) throws BinaryStorageException, ObjectNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			return Files.newInputStream(path);
		} catch (Exception e) {
			throw new BinaryStorageException(e);
		}
	}

	@Override
	public String put(InputStream content) throws BinaryStorageException, ObjectAlreadyExistsException, ObjectCollisionException {
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
		} catch (IOException | IdentifierGeneratorException e) {
			throw new BinaryStorageException(e);
		}
	}

	@Override
	public void check(String identifier) throws BinaryStorageException, ObjectNotFoundException, ObjectCorruptedException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		String check;
		try (InputStream input = Files.newInputStream(path)) {
			check = generator.generate(input);
		} catch (IOException | IdentifierGeneratorException e) {
			throw new BinaryStorageException(e);
		}
		if (!check.equals(identifier)) {
			throw new ObjectCorruptedException("The object with id [" + identifier
					+ "] is CORRUPTED. The stored object's content has generate a wrong identifier [" + check + "]");
		}
	}

	@Override
	public void delete(String identifier) throws BinaryStorageException, ObjectNotFoundException {
		Path path = Paths.get(base.toString(), identifier.substring(0, 4), identifier);
		if (!Files.exists(path)) {
			throw new ObjectNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
		}
		try {
			Files.delete(path);
		} catch (Exception e) {
			throw new BinaryStorageException(e);
		}
	}

	@Override
	public String generate(InputStream content) throws BinaryStorageException {
		try {
			String hash = generator.generate(content);
			return hash;
		} catch (Exception e) {
			throw new BinaryStorageException("Unable to generate an identifier for this content: " + e.getMessage(), e);
		}
	}

}
