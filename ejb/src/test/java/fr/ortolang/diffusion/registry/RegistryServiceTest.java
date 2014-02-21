package fr.ortolang.diffusion.registry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;

public class RegistryServiceTest {
	
	private Logger logger = Logger.getLogger(RegistryServiceTest.class.getName());

	private RegistryService registry;
	
	@Before
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
		try {
			registry = new RegistryServiceBean();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
		logger.log(Level.INFO, "clearing environment");
	}

	@Test
	public void testLookup() {
		String key = "mytestkey";
		OrtolangObjectIdentifier doi = new OrtolangObjectIdentifier("Test", "testing", "atestid");
		try {
			registry.create(key, doi);
			OrtolangObjectIdentifier doi2 = registry.lookup(key).getIdentifier();
			assertTrue(doi2.equals(doi));
		} catch (RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = KeyNotFoundException.class)
	public void testLookupUexistingKey() throws KeyNotFoundException {
		String key = "unexistingkey";
		try {
		    registry.lookup(key);
		    fail("This lookup should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = KeyAlreadyExistsException.class)
	public void testBindExistingKey() throws KeyAlreadyExistsException {
		String key = "existingkey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		try {
		    registry.create(key, doi1);
		    registry.create(key, doi2);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testHide() {
		String key = "hidekey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.create(key, doi1);
		    RegistryEntry rentry = registry.lookup(key);
		    OrtolangObjectIdentifier doi = rentry.getIdentifier();
			assertTrue(doi1.equals(doi));
			registry.hide(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException e ) {
			fail(e.getMessage());
		}	
		try {
		    RegistryEntry entry = registry.lookup(key);
		    assertTrue(entry.isHidden());
		    assertTrue(!entry.isLocked());
		    assertTrue(!entry.isDeleted());
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testHideUnexisting() throws KeyNotFoundException {
		String key = "hideunexistingkey";
		try {
		    registry.hide(key);
		    fail("the hide should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testLock() {
		String key = "lockkey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.create(key, doi1);
		    RegistryEntry rentry = registry.lookup(key);
		    OrtolangObjectIdentifier doi = rentry.getIdentifier();
			assertTrue(doi1.equals(doi));
			registry.lock(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException e ) {
			fail(e.getMessage());
		}	
		try {
		    RegistryEntry entry = registry.lookup(key);
		    assertTrue(!entry.isHidden());
		    assertTrue(entry.isLocked());
		    assertTrue(!entry.isDeleted());
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testLockUnexisting() throws KeyNotFoundException {
		String key = "lockunexistingkey";
		try {
		    registry.lock(key);
		    fail("the lock should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testDelete() {
		String key = "deletekey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.create(key, doi1);
		    RegistryEntry rentry = registry.lookup(key);
		    OrtolangObjectIdentifier doi = rentry.getIdentifier();
			assertTrue(doi1.equals(doi));
			registry.delete(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException e ) {
			fail(e.getMessage());
		}	
		try {
		    RegistryEntry entry = registry.lookup(key);
		    assertTrue(!entry.isHidden());
		    assertTrue(!entry.isLocked());
		    assertTrue(entry.isDeleted());
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testDeleteUnexisting() throws KeyNotFoundException {
		String key = "deleteunexistingkey";
		try {
		    registry.delete(key);
		    fail("the delete should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testList() {
		String key1 = "key1";
		String key2 = "key2";
		String key3 = "key3";
		String key4 = "key4";
		String key5 = "key5";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		OrtolangObjectIdentifier doi3 = new OrtolangObjectIdentifier("Test", "testing", "atestid3");
		OrtolangObjectIdentifier doi4 = new OrtolangObjectIdentifier("Test", "testing", "atestid4");
		OrtolangObjectIdentifier doi5 = new OrtolangObjectIdentifier("Test", "testing", "atestid5");
		try {
		    registry.create(key1, doi1);
		    registry.create(key2, doi2);
		    registry.create(key3, doi3);
		    registry.create(key4, doi4);
		    registry.create(key5, doi5);
		    List<RegistryEntry> entries = registry.list(0, 10, ".*", true);
		    long size = registry.count(".*", true);
		    assertTrue(entries.size() == 5);
		    assertTrue(size == 5);
		    
		    registry.hide(key2);
		    entries = registry.list(0, 10, ".*", true);
		    size = registry.count(".*", true);
		    assertTrue(entries.size() == 4);
		    assertTrue(size == 4);
		    entries = registry.list(0, 10, ".*", false);
		    size = registry.count(".*", false);
		    assertTrue(entries.size() == 5);
		    assertTrue(size == 5);
		    
		    registry.delete(key4);
		    entries = registry.list(0, 10, ".*", true);
		    size = registry.count(".*", true);
		    assertTrue(entries.size() == 3);
		    assertTrue(size == 3);
		    entries = registry.list(0, 10, ".*", false);
		    size = registry.count(".*", false);
		    assertTrue(entries.size() == 5);
		    assertTrue(size == 5);
		    
		    registry.lock(key5);
		    entries = registry.list(0, 10, ".*", true);
		    size = registry.count(".*", true);
		    assertTrue(entries.size() == 3);
		    assertTrue(size == 3);
		    entries = registry.list(0, 10, ".*", false);
		    size = registry.count(".*", false);
		    assertTrue(entries.size() == 5);
		    assertTrue(size == 5);
		    
		    registry.show(key2);
		    entries = registry.list(0, 10, ".*", true);
		    size = registry.count(".*", true);
		    assertTrue(entries.size() == 4);
		    assertTrue(size == 4);
		    entries = registry.list(0, 10, ".*", false);
		    size = registry.count(".*", false);
		    assertTrue(entries.size() == 5);
		    assertTrue(size == 5);
		    
		} catch (RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException e ) {
			fail(e.getMessage());
		}
	}

}
