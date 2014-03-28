package fr.ortolang.diffusion.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

@RunWith(Arquillian.class)
public class RegistryServiceTest {
	
	private static Logger logger = Logger.getLogger(RegistryServiceTest.class.getName());

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
		jar.addAsResource("config.properties");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        logger.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-registry.ear");
		ear.addAsModule(jar);
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.jmock:jmock-junit4:2.5.1").withTransitivity().asFile());
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
    }
 
	@Before
	public void setup() throws Exception {
		logger.log(Level.INFO, "Setting up test environment, clearing data");
		utx.begin();
	    em.joinTransaction();
		em.createQuery("delete from RegistryEntry").executeUpdate();
	    em.createQuery("delete from RegistryTag").executeUpdate();
	    utx.commit();
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
			registry.register(key, doi);
			OrtolangObjectIdentifier doi2 = registry.lookup(key);
			assertTrue(doi2.equals(doi));
		} catch (RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException | IdentifierAlreadyRegisteredException e) {
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
		    registry.register(key, doi1);
		    registry.register(key, doi2);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = IdentifierAlreadyRegisteredException.class)
	public void testBindExistingIdentifier() throws IdentifierAlreadyRegisteredException {
		String key1 = "key1";
		String key2 = "key2";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key1, doi1);
		    registry.register(key2, doi1);
		    fail("This bind should have raised an exception");
		} catch (RegistryServiceException | KeyAlreadyExistsException e ) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testHide() {
		String key = "hidekey";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key, doi1);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.hide(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
			boolean hidden = registry.isHidden(key);
			assertTrue(hidden);
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
		String lock = "lockowner";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		try {
		    registry.register(key, doi1);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.lock(key, lock);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
			boolean locked = registry.isLocked(key);
		    assertTrue(locked);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test (expected = KeyNotFoundException.class)
	public void testLockUnexisting() throws KeyNotFoundException {
		String key = "lockunexistingkey";
		String lock = "lockowner";
		try {
		    registry.lock(key, lock);
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
		    registry.register(key, doi1);
		    OrtolangObjectIdentifier doi = registry.lookup(key);
			assertTrue(doi1.equals(doi));
			registry.delete(key);
		} catch (RegistryServiceException | KeyNotFoundException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}	
		try {
		    boolean deleted = registry.isDeleted(key);
		    assertTrue(deleted);
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
		String lock = "lockowner";
		OrtolangObjectIdentifier doi1 = new OrtolangObjectIdentifier("Test", "testing", "atestid1");
		OrtolangObjectIdentifier doi2 = new OrtolangObjectIdentifier("Test", "testing", "atestid2");
		OrtolangObjectIdentifier doi3 = new OrtolangObjectIdentifier("Test", "testing", "atestid3");
		OrtolangObjectIdentifier doi4 = new OrtolangObjectIdentifier("Test", "testing", "atestid4");
		OrtolangObjectIdentifier doi5 = new OrtolangObjectIdentifier("Test", "testing", "atestid5");
		try {
		    registry.register(key1, doi1);
		    registry.register(key2, doi2);
		    registry.register(key3, doi3);
		    registry.register(key4, doi4);
		    registry.register(key5, doi5);
		    List<String> entries = registry.list(0, 10, "");
		    long size = registry.count("");
		    assertEquals(5,entries.size());
		    assertEquals(5,size);
		    
		    registry.hide(key2);
		    entries = registry.list(0, 10, "");
		    size = registry.count("");
		    assertEquals(4,entries.size());
		    assertEquals(4,size);
		    
		    registry.delete(key4);
		    entries = registry.list(0, 10, "");
		    size = registry.count("");
		    assertEquals(3,entries.size());
		    assertEquals(3,size);
		    
		    registry.lock(key5, lock);
		    entries = registry.list(0, 10, "");
		    size = registry.count("");
		    assertEquals(3,entries.size());
		    assertEquals(3,size);
		    
		    registry.show(key2);
		    entries = registry.list(0, 10, "");
		    size = registry.count("");
		    assertEquals(4,entries.size());
		    assertEquals(4,size);
		    
		} catch (RegistryServiceException | KeyAlreadyExistsException | KeyNotFoundException | IdentifierAlreadyRegisteredException e ) {
			fail(e.getMessage());
		}
	}

}
