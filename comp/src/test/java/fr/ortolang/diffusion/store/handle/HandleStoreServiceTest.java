package fr.ortolang.diffusion.store.handle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HandleStoreServiceTest {
	
	private Logger logger = Logger.getLogger(HandleStoreServiceTest.class.getName());
	private HandleStoreServiceBean service;
	
	@Before
	public void setup() {
		try {
			service = new HandleStoreServiceBean();
			service.init();
			service.setTraceEnabled();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
		
	}

	@Test
	public void testCreate() {
		try {
			service.create("test-666", "http://home.jayblanc.fr/photo");
			
			assertTrue(service.exists("test-666"));
			assertFalse(service.exists("test-777"));
			
			String res = service.read("test-666");
			logger.log(Level.INFO, "received handle value: " + res);
			service.delete("test-666");
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}

}
