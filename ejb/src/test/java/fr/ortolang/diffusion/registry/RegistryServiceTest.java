package fr.ortolang.diffusion.registry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;

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
		RegistryEntry entry = new RegistryEntry(key, RegistryEntryState.USED, doi);
		try {
			registry.create(entry);
			OrtolangObjectIdentifier doi2 = registry.lookup(key).getIdentifier();
			assertTrue(doi2.equals(doi));
		} catch (RegistryServiceException | EntryAlreadyExistsException | EntryNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = EntryNotFoundException.class)
	public void testLookupUexistingKey() throws EntryNotFoundException {
		String key = "unexistingkey";
		try {
		    registry.lookup(key);
		    fail("This lookup should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = EntryAlreadyExistsException.class)
	public void testBindExistingKey() throws EntryAlreadyExistsException {
		String key = "existingkey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		RegistryEntry entry1 = new RegistryEntry(key, RegistryEntryState.USED, doi1);
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		RegistryEntry entry2 = new RegistryEntry(key, RegistryEntryState.USED, doi2);
		try {
		    registry.create(entry1);
		    registry.create(entry2);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testRebind() {
		String key = "rebindkey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		RegistryEntry entry = new RegistryEntry(key, RegistryEntryState.USED, doi1);
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		try {
		    registry.create(entry);
		    RegistryEntry rentry = registry.lookup(key);
		    OrtolangObjectIdentifier doi3 = rentry.getIdentifier();
			assertTrue(doi1.equals(doi3));
			
			rentry.setIdentifier(doi2);
		    registry.update(rentry);
		    doi3 = registry.lookup(key).getIdentifier();
		    assertTrue(doi2.equals(doi3));
		} catch (RegistryServiceException | EntryNotFoundException | EntryAlreadyExistsException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = EntryNotFoundException.class)
	public void testRebindUnexisting() throws EntryNotFoundException {
		String key = "rebindunexistingkey";
		OrtolangObjectIdentifier doi = new OrtolangObjectIdentifier("Test", "testing", "atestid");
		RegistryEntry entry = new RegistryEntry(key, RegistryEntryState.USED, doi);
		try {
		    registry.update(entry);
		    fail("the rebind shoudl have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = EntryNotFoundException.class)
	public void testUnbind() throws EntryNotFoundException {
		String key = "unbindkey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		RegistryEntry entry = new RegistryEntry(key, RegistryEntryState.USED, doi1);
		try {
		    registry.create(entry);
		    RegistryEntry rentry = registry.lookup(key);
		    OrtolangObjectIdentifier doi = rentry.getIdentifier();
			assertTrue(doi1.equals(doi));
			registry.delete(key);
		} catch (RegistryServiceException | EntryNotFoundException | EntryAlreadyExistsException e ) {
			fail(e.getMessage());
		}	
		try {
		    registry.lookup(key);
		    fail("the lookup should have raised an exception");
		} catch (RegistryServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = EntryNotFoundException.class)
	public void testUnbindUnexisting() throws EntryNotFoundException {
		String key = "unbindunexistingkey";
		try {
		    registry.delete(key);
		    fail("the unbind shoudl have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}

}
