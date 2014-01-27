package fr.ortolang.diffusion.registry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.DiffusionObjectIdentifier;

public class InMemoryRegistryTest {
	
	private Logger logger = Logger.getLogger(InMemoryRegistryTest.class.getName());

	private RegistryService registry;
	
	@Before
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
		try {
			registry = InMemoryRegistry.getInstance();
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
		DiffusionObjectIdentifier doi = new DiffusionObjectIdentifier("Test", "testing", "atestid");
		try {
			registry.bind(key, doi);
			DiffusionObjectIdentifier doi2 = registry.lookup(key);
			assertTrue(doi2.equals(doi));
		} catch (RegistryServiceException | KeyAlreadyBoundException | KeyNotFoundException e) {
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
	
	@Test(expected = KeyAlreadyBoundException.class)
	public void testBindExistingKey() throws KeyAlreadyBoundException {
		String key = "existingkey";
		DiffusionObjectIdentifier doi1 = new DiffusionObjectIdentifier("Test", "testing", "atestid1");
		DiffusionObjectIdentifier doi2 = new DiffusionObjectIdentifier("Test", "testing", "atestid2");
		try {
		    registry.bind(key, doi1);
		    registry.bind(key, doi2);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testRebind() {
		String key = "rebindkey";
		DiffusionObjectIdentifier doi1 = new DiffusionObjectIdentifier("Test", "testing", "atestid1");
		DiffusionObjectIdentifier doi2 = new DiffusionObjectIdentifier("Test", "testing", "atestid2");
		try {
		    registry.bind(key, doi1);
		    DiffusionObjectIdentifier doi3 = registry.lookup(key);
			assertTrue(doi1.equals(doi3));
			
		    registry.rebind(key, doi2);
		    doi3 = registry.lookup(key);
		    assertTrue(doi2.equals(doi3));
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyBoundException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testRebindUnexisting() throws KeyNotFoundException {
		String key = "rebindunexistingkey";
		DiffusionObjectIdentifier doi = new DiffusionObjectIdentifier("Test", "testing", "atestid");
		try {
		    registry.rebind(key, doi);
		    fail("the rebind shoudl have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testUnbind() throws KeyNotFoundException {
		String key = "unbindkey";
		DiffusionObjectIdentifier doi1 = new DiffusionObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.bind(key, doi1);
		    DiffusionObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.unbind(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyBoundException e ) {
			fail(e.getMessage());
		}	
		try {
		    registry.lookup(key);
		    fail("the lookup should have raised an exception");
		} catch (RegistryServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testUnbindUnexisting() throws KeyNotFoundException {
		String key = "unbindunexistingkey";
		try {
		    registry.unbind(key);
		    fail("the unbind shoudl have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}

}
