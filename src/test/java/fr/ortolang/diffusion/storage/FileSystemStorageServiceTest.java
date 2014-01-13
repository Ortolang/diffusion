package fr.ortolang.diffusion.storage;

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
		DigitalObject object1 = new DigitalObject(new ByteArrayInputStream("Sample Digital Content v1.0".getBytes()));
		DigitalObject object2 = new DigitalObject(new ByteArrayInputStream("Sample Digital Content v1.0".getBytes()));
		DigitalObject object3 = new DigitalObject(new ByteArrayInputStream("Sample Digital Content v1.1".getBytes()));
		DigitalObject object4 = new DigitalObject(new ByteArrayInputStream("Another Sample Digital Content".getBytes()));

		try {
			String hash1 = service.generate(object1);
			logger.log(Level.INFO, "Hash for object1: " + hash1);
			String hash2 = service.generate(object2);
			logger.log(Level.INFO, "Hash for object2: " + hash2);
			String hash3 = service.generate(object3);
			logger.log(Level.INFO, "Hash for object3: " + hash3);
			String hash4 = service.generate(object4);
			logger.log(Level.INFO, "Hash for object4: " + hash4);

			assertTrue(hash1.equals(hash2));
			assertFalse(hash1.equals(hash3));
			assertFalse(hash1.equals(hash4));
			assertFalse(hash3.equals(hash4));
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	@Test
	public void testInsertAndRetrieveWithMockedIdentifierGenerator() {
		final StorageIdentifierGenerator generator = context.mock(StorageIdentifierGenerator.class);
		service.setStorageIdentifierGenerator(generator);
		final DigitalObject newObject = new DigitalObject(new ByteArrayInputStream("Sample Digital Content v1.0".getBytes()));

		try {
			context.checking(new Expectations() {
				{
					oneOf(generator).generate(newObject.getData());
					will(returnValue("123456789abcdef"));
				}
			});
		} catch (Exception e1) {
			fail(e1.getMessage());
		}

		String identifier;
		DigitalObject retrieveObject;

		try {
			identifier = service.put(newObject);
			assertTrue(identifier.equals("123456789abcdef"));

			retrieveObject = service.get(identifier);
			System.out.println(newObject.getData().available());
			System.out.println(newObject.getData().markSupported());
			newObject.getData().reset();
			System.out.println(newObject.getData().available());
			assertTrue(FileSystemStorageServiceTest.streamEquals(newObject.getData(), retrieveObject.getData()));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		context.assertIsSatisfied();
	}

	private static boolean streamEquals(InputStream input1, InputStream input2) throws IOException {
		if (!(input1 instanceof BufferedInputStream)) {
			input1 = new BufferedInputStream(input1);
		}
		if (!(input2 instanceof BufferedInputStream)) {
			input2 = new BufferedInputStream(input2);
		}

		int ch = input1.read();
		while (-1 != ch) {
			int ch2 = input2.read();
			if (ch != ch2) {
				return false;
			}
			ch = input1.read();
		}

		int ch2 = input2.read();
		return ch2 == -1;
	}

}
