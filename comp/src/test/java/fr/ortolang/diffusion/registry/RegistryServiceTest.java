package fr.ortolang.diffusion.registry;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

@RunWith(Arquillian.class)
public class RegistryServiceTest {
	
	private static final Logger LOGGER = Logger.getLogger(RegistryServiceTest.class.getName());

    @PersistenceContext
    private EntityManager em;
    
    @Resource(name="java:jboss/UserTransaction")
    private UserTransaction utx;
    
    @EJB
	private RegistryService registry;
	
	@Deployment
    public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "registry.jar");
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.registry");
		jar.addPackage("fr.ortolang.diffusion.registry.entity");
		jar.addClass("fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory");
		jar.addAsResource("config.properties");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-registry.ear");
		ear.addAsModule(jar);
		LOGGER.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
    }
 
	@Before
	public void setup() throws Exception {
		LOGGER.log(Level.INFO, "Setting up test environment, clearing data");
		utx.begin();
	    em.joinTransaction();
		em.createQuery("delete from RegistryEntry").executeUpdate();
	    utx.commit();
	}
	
	@After
	public void tearDown() {
		LOGGER.log(Level.INFO, "clearing environment");
	}

	@Test
	public void testLookup() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "mytestkey";
		String author = "moi";
		OrtolangObjectIdentifier doi = new OrtolangObjectIdentifier("Test", "testing", "atestid");
		try {
			registry.register(key, doi, author);
			OrtolangObjectIdentifier doi2 = registry.lookup(key);
			assertTrue(doi2.equals(doi));
		} catch (RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException | IdentifierAlreadyRegisteredException e) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test(expected = KeyNotFoundException.class)
	public void testLookupUnexistingKey() throws KeyNotFoundException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "unexistingkey";
		try {
		    registry.lookup(key);
		    fail("This lookup should have raised an exception");
		} catch (RegistryServiceException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test(expected = KeyAlreadyExistsException.class)
	public void testBindExistingKey() throws KeyAlreadyExistsException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "existingkey";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		try {
		    registry.register(key, doi1, author);
		    registry.register(key, doi2, author);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test(expected = IdentifierAlreadyRegisteredException.class)
	public void testBindExistingIdentifier() throws IdentifierAlreadyRegisteredException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key1 = "key1";
		String key2 = "key2";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key1, doi1, author);
		    registry.register(key2, doi1, author);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException | KeyAlreadyExistsException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test
	public void testHide() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "hidekey";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key, doi1, author);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.hide(key);
		} catch (KeyLockedException | RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
			boolean hidden = registry.isHidden(key);
			assertTrue(hidden);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testHideUnexisting() throws KeyNotFoundException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "hideunexistingkey";
		try {
		    registry.hide(key);
		    fail("the hide should have raised an exception");
		} catch (KeyLockedException | RegistryServiceException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test
	public void testLock() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "lockkey";
		String lock = "lockowner";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key, doi1, author);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.lock(key, lock);
		} catch (KeyLockedException | RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
			boolean locked = registry.isLocked(key);
		    assertTrue(locked);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testLockUnexisting() throws KeyNotFoundException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "lockunexistingkey";
		String lock = "lockowner";
		try {
		    registry.lock(key, lock);
		    fail("the lock should have raised an exception");
		} catch (KeyLockedException | RegistryServiceException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testDelete() throws KeyNotFoundException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "deletekey";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key, doi1, author);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.delete(key);
		} catch (KeyLockedException | RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
		    registry.lookup(key);
			fail("lookup should fail");
		} catch (RegistryServiceException e) {
			fail("this is not the good exception");
		} finally {
			loginContext.logout();
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testDeleteUnexisting() throws KeyNotFoundException, LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key = "deleteunexistingkey";
		try {
		    registry.delete(key);
		    fail("the delete should have raised an exception");
		} catch (KeyLockedException | RegistryServiceException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testList() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		String key1 = "key1";
		String key2 = "key2";
		String key3 = "key3";
		String key4 = "key4";
		String key5 = "key5";
		String lock = "lockowner";
		String author = "moi";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		OrtolangObjectIdentifier doi3 = new OrtolangObjectIdentifier("Test", "testing", "atestid3");
		OrtolangObjectIdentifier doi4 = new OrtolangObjectIdentifier("Test", "testing", "atestid4");
		OrtolangObjectIdentifier doi5 = new OrtolangObjectIdentifier("Test", "testing", "atestid5");
		try {
		    registry.register(key1, doi1, author);
		    registry.register(key2, doi2, author);
		    registry.register(key3, doi3, author);
		    registry.register(key4, doi4, author);
		    registry.register(key5, doi5, author);
		    List<String> entries = registry.list(0, 10, "", null, false);
		    long size = registry.count("", null, false);
		    assertEquals(5,entries.size());
		    assertEquals(5,size);
		    
		    registry.hide(key2);
		    entries = registry.list(0, 10, "", null, false);
		    size = registry.count("", null, false);
		    assertEquals(4,entries.size());
		    assertEquals(4,size);
		    
		    registry.delete(key4);
		    entries = registry.list(0, 10, "", null, false);
		    size = registry.count("", null, false);
		    assertEquals(3,entries.size());
		    assertEquals(3,size);
		    
		    registry.lock(key5, lock);
		    entries = registry.list(0, 10, "", null, false);
		    size = registry.count("", null, false);
		    assertEquals(3,entries.size());
		    assertEquals(3,size);
		    
		    registry.show(key2);
		    entries = registry.list(0, 10, "", null, false);
		    size = registry.count("", null, false);
		    assertEquals(4,entries.size());
		    assertEquals(4,size);
		    
		} catch (KeyLockedException | RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		} finally {
			loginContext.logout();
		}
	}
	
//	@Test
//	public void testConcurrentStateWriteAndRead() throws LoginException, IllegalStateException, SecurityException, SystemException {
//		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
//		loginContext.login();
//		String key1 = "key1";
//		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
//		String author = "moi";
//		ExecutorService pool = Executors.newFixedThreadPool(1);
//		try {
//			utx.begin();
//			registry.register(key1, doi1, author);
//			utx.commit();
//			utx.begin();
//			registry.setPublicationStatus(key1, OrtolangObjectState.Status.PUBLISHED.value());
//			Future<String> result = pool.submit(new RegistryEntryStateReader(key1));
//			Thread.sleep(1000);
//			utx.commit();
//			assertEquals(OrtolangObjectState.Status.PUBLISHED.value(), result.get());
//		} catch ( Exception e ) {
//			fail(e.getMessage());
//		} finally {
//			loginContext.logout();
//		}
//		
//	}
	
	class RegistryEntryStateReader implements Callable<String> {
		
		private String key;
		
		public RegistryEntryStateReader(String key) {
			this.key = key;
		}
		
		@Override
		public String call() throws Exception {
			LOGGER.log(Level.INFO, "Starting registry entry status reader");
			LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
			loginContext.login();
			String status = registry.getPublicationStatus(key);
			loginContext.logout();
			LOGGER.log(Level.INFO, "Registry entry status reader done: status=" + status);
			return status;
		}
		
	}

}
