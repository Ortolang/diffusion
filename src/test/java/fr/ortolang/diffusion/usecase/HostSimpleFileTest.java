package fr.ortolang.diffusion.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.DiffusionObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.InMemoryCoreService;
import fr.ortolang.diffusion.core.entity.ObjectContainer;
import fr.ortolang.diffusion.registry.InMemoryRegistry;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.LocalBinaryStoreService;

public class HostSimpleFileTest {
	
	private static final String BINARY_STORE_BASE = "/tmp/ortolang-storage/" + System.currentTimeMillis();
	
	private Logger logger = Logger.getLogger(HostSimpleFileTest.class.getName());
	
	private RegistryService registry;
	private BinaryStoreService store;
	private CoreService core;
	

	@Before
	public void setup() {
		try {
			store = new LocalBinaryStoreService(Paths.get(BINARY_STORE_BASE));
			registry = InMemoryRegistry.getInstance();
			core = InMemoryCoreService.getInstance();
			((InMemoryCoreService)core).setRegistryService(registry);
			((InMemoryCoreService)core).setBinaryStoreService(store);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	@After
	public void tearDown() {
		try {
			Files.walkFileTree(Paths.get(BINARY_STORE_BASE), new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					logger.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				
			});
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	@Test
	public void testHostSimpleFile() throws URISyntaxException {
		Path origin = Paths.get(getClass().getClassLoader().getResource("file1.jpg").getPath());
		logger.log(Level.INFO, "Origin file to insert in container : " + origin.toString());
		Path destination = Paths.get("/tmp/" + System.currentTimeMillis());
		logger.log(Level.INFO, "Destination file for retrieving content from container : " + destination.toString());
		String streamName = "file1";
		String key = UUID.randomUUID().toString();
		
		//Create the DiffusionObject 
		try (InputStream streamData = Files.newInputStream(origin) ) {
			core.createContainer(key, "A test container");
			core.addDataStreamToContainer(key, streamName, streamData);
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
		
		//Check that the object is registered in the repository
		try {
			DiffusionObjectIdentifier identifier = registry.lookup(key);
			assertEquals(identifier.getService(), CoreService.SERVICE_NAME);
			assertEquals(identifier.getType(), ObjectContainer.OBJECT_TYPE);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
		
		//Retrieve this object using the key 
		try {
			ObjectContainer container = core.getContainer(key);
			String hash = container.getStreams().get(streamName);
			logger.log(Level.INFO, "stream has been stored in container with hash: " + hash);
			InputStream input = core.getDataStreamFromContainer(key, streamName);
			Files.copy(input, destination);
			input.close();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
		
		//Compare origin and destination : 
		try {
			InputStream input1 = Files.newInputStream(origin);
			InputStream input2 = Files.newInputStream(destination);
			assertTrue(IOUtils.contentEquals(input1, input2));
			input1.close();
			input2.close();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
		
		//Delete destination
		try {
			Files.delete(destination);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
