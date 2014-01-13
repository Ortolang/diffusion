package fr.ortolang.diffusion.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileSystemStorageServiceTest {

	private Logger logger = Logger.getLogger(FileSystemStorageServiceTest.class.getName());

	private static Mockery context;
	private FileSystemStorageService service;

	@BeforeClass
	public static void init() {
		context = new Mockery();
	}

	@Before
	public void setup() {
		try {
			service = new FileSystemStorageService(Paths.get("/tmp/ortolang-storage/" + System.currentTimeMillis()));
		} catch (Exception e) {
			fail(e.getMessage());
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

	@Test
	public void testNormalInsertAndRetrieve() {
		final StorageIdentifierGenerator generator = context.mock(StorageIdentifierGenerator.class);
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

}
