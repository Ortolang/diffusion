package fr.ortolang.diffusion.store.binary;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalBinaryStoreTest {

	private Logger logger = Logger.getLogger(LocalBinaryStoreTest.class.getName());
	private LocalBinaryStoreService service;
	
	@Before
	public void setup() {
		try {
			service = new LocalBinaryStoreService(Paths.get("/tmp/ortolang-storage/" + System.currentTimeMillis()));
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
	
	@Test(expected = DataNotFoundException.class)  
	public void testRetrieveUnexistingObject() throws BinaryStoreServiceException, DataNotFoundException {
		service.get("unexistingidentifier");
	}

	@Test
	public void testInsertUnexistingObject() {
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			String identifier = service.put(new ByteArrayInputStream(content));
			InputStream is = service.get(identifier);
			assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(content), is));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testInsertExistingObject() throws BinaryStoreServiceException {
		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
		final byte[] content2 = "Sample Digital Content v1.0".getBytes();
		
		try {
			String id1 = service.put(new ByteArrayInputStream(content1));
			String id2 = service.put(new ByteArrayInputStream(content2));
			assertEquals(id1, id2);
		} catch (DataCollisionException e) {
			fail(e.getMessage());
		} 
	}
	
//	@Test(expected = DataCollisionException.class)  
//	public void testInsertCollisionObject() throws BinaryStoreServiceException, DataCollisionException {
//		service.setStorageIdentifierGenerator(generator);
//		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
//		final byte[] content2 = "Sample Digital Content that generate a collision".getBytes();
//		
//		try {
//			context.checking(new Expectations() {
//				{
//					allowing(generator).generate(with(any(InputStream.class)));
//					will(returnValue("123456789abcdef"));
//				}
//			});
//		} catch (Exception e) {
//			fail(e.getMessage());
//		}
//		
//		service.put(new ByteArrayInputStream(content1));
//		service.put(new ByteArrayInputStream(content2));
//		 
//	}
	
	@Test(expected = DataNotFoundException.class)
	public void testCheckUnexistingObject() throws DataCorruptedException, BinaryStoreServiceException, DataNotFoundException {
		service.check("unexistingidentifier");
	}
	
	@Test
	public void testCheckExistingObject() {
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			String identifier1 = service.put(new ByteArrayInputStream(content));
			service.check(identifier1);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
//	@Test(expected = DataCorruptedException.class)
//	public void testCheckExistingCorruptedObject() throws DataNotFoundException, BinaryStoreServiceException, DataCollisionException, DataCorruptedException {
//		service.setStorageIdentifierGenerator(generator);
//		final byte[] content = "Sample Digital Content v1.0".getBytes();
//
//		try {
//			context.checking(new Expectations() {
//				{
//					oneOf(generator).generate(with(any(InputStream.class)));
//					will(returnValue("123456789abcdef"));
//					oneOf(generator).generate(with(any(InputStream.class)));
//					will(returnValue("fedcba987654321"));
//				}
//			});
//		} catch (Exception e1) {
//			fail(e1.getMessage());
//		}
//
//		String identifier1 = service.put(new ByteArrayInputStream(content));
//		assertTrue(identifier1.equals("123456789abcdef"));
//
//		service.check(identifier1);
//		
//		context.assertIsSatisfied();
//	}
}
