package fr.ortolang.diffusion.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileSystemStorageServiceTest {

	private Logger logger = Logger.getLogger(FileSystemStorageServiceTest.class.getName());

	private Mockery context;
	private StorageIdentifierGenerator generator;
	private FileSystemStorageService service;
	
	@Before
	public void setup() {
		try {
			context = new Mockery();
			generator = context.mock(StorageIdentifierGenerator.class);
			service = new FileSystemStorageService(Paths.get("/tmp/ortolang-storage/" + System.currentTimeMillis()));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
		try {
			Files.walkFileTree(service.getBase(), new FileVisitor<Path>() {
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
	public void testHash() {
		byte[] content1 = "Sample Digital Content v1.0".getBytes();
		byte[] content2 = "Sample Digital Content v1.0".getBytes();
		byte[] content3 = "Sample Digital Content v1.1".getBytes();
		byte[] content4 = "Another Sample Digital Content".getBytes();

		try {
			String hash1 = service.generate(new ByteArrayInputStream(content1));
			String hash2 = service.generate(new ByteArrayInputStream(content2));
			String hash3 = service.generate(new ByteArrayInputStream(content3));
			String hash4 = service.generate(new ByteArrayInputStream(content4));
			String hash5 = service.generate(new ByteArrayInputStream(content1));
			String hash6 = service.generate(new ByteArrayInputStream(content1));
			
			assertEquals(hash1,hash2);
			assertEquals(hash1,hash5);
			assertEquals(hash1,hash6);
			assertEquals(hash5,hash6);
			assertFalse(hash1.equals(hash3));
			assertFalse(hash1.equals(hash4));
			assertFalse(hash3.equals(hash4));
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	
	@Test(expected = ObjectNotFoundException.class)  
	public void testRetrieveUnexistingObject() throws StorageServiceException {
		service.get("unexistingidentifier");
	}

	@Test
	public void testInsertUnexistingObject() {
		service.setStorageIdentifierGenerator(generator);
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			context.checking(new Expectations() {
				{
					oneOf(generator).generate(with(any(InputStream.class)));
					will(returnValue("123456789abcdef"));
				}
			});
		} catch (Exception e1) {
			fail(e1.getMessage());
		}

		String identifier;

		try {
			identifier = service.put(new ByteArrayInputStream(content));
			assertTrue(identifier.equals("123456789abcdef"));

			InputStream is = service.get(identifier);
			assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(content), is));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		context.assertIsSatisfied();
	}
	
	@Test(expected = ObjectAlreadyExistsException.class)  
	public void testInsertExistingObject() throws ObjectAlreadyExistsException, StorageServiceException {
		service.setStorageIdentifierGenerator(generator);
		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
		final byte[] content2 = "Sample Digital Content v1.0".getBytes();
		
		try {
			context.checking(new Expectations() {
				{
					allowing(generator).generate(with(any(InputStream.class)));
					will(returnValue("123456789abcdef"));
				}
			});
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		try {
			service.put(new ByteArrayInputStream(content1));
			service.put(new ByteArrayInputStream(content2));
		} catch (ObjectCollisionException e) {
			fail(e.getMessage());
		} 
	}
	
	@Test(expected = ObjectCollisionException.class)  
	public void testInsertCollisionObject() throws StorageServiceException {
		service.setStorageIdentifierGenerator(generator);
		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
		final byte[] content2 = "Sample Digital Content that generate a collision".getBytes();
		
		try {
			context.checking(new Expectations() {
				{
					allowing(generator).generate(with(any(InputStream.class)));
					will(returnValue("123456789abcdef"));
				}
			});
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		try {
			service.put(new ByteArrayInputStream(content1));
			service.put(new ByteArrayInputStream(content2));
		} catch (ObjectAlreadyExistsException e) {
			fail(e.getMessage());
		} 
	}
	
	@Test(expected = ObjectNotFoundException.class)
	public void testCheckUnexistingObject() throws ObjectCorruptedException, StorageServiceException {
		service.check("unexistingidentifier");
	}
	
	@Test
	public void testCheckExistingObject() {
		service.setStorageIdentifierGenerator(generator);
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			context.checking(new Expectations() {
				{
					allowing(generator).generate(with(any(InputStream.class)));
					will(returnValue("123456789abcdef"));
				}
			});
		} catch (Exception e1) {
			fail(e1.getMessage());
		}

		try {
			String identifier1 = service.put(new ByteArrayInputStream(content));
			assertTrue(identifier1.equals("123456789abcdef"));

			service.check(identifier1);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		context.assertIsSatisfied();
	}
	
	@Test(expected = ObjectCorruptedException.class)
	public void testCheckExistingCorruptedObject() throws ObjectNotFoundException, StorageServiceException {
		service.setStorageIdentifierGenerator(generator);
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			context.checking(new Expectations() {
				{
					oneOf(generator).generate(with(any(InputStream.class)));
					will(returnValue("123456789abcdef"));
					oneOf(generator).generate(with(any(InputStream.class)));
					will(returnValue("fedcba987654321"));
				}
			});
		} catch (Exception e1) {
			fail(e1.getMessage());
		}

		String identifier1 = service.put(new ByteArrayInputStream(content));
		assertTrue(identifier1.equals("123456789abcdef"));

		service.check(identifier1);
		
		context.assertIsSatisfied();
	}
}
