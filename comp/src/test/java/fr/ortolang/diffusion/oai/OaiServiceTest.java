package fr.ortolang.diffusion.oai;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.oai.entity.Record;

@RunWith(Arquillian.class)
public class OaiServiceTest {

	private static final Logger LOGGER = Logger.getLogger(OaiServiceTest.class.getName());

    @PersistenceContext
    private EntityManager em;
    
    @Resource(name="java:jboss/UserTransaction")
    private UserTransaction utx;
    
    @EJB
	private OaiService oai;
	
	@Deployment
    public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "registry.jar");
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.oai");
		jar.addPackage("fr.ortolang.diffusion.oai.entity");
		jar.addAsResource("config.properties");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-oai.ear");
		ear.addAsModule(jar);
		LOGGER.log(Level.INFO, "Created EAR for testing oai : " + ear.toString(true));

		return ear;
    }
 
	@Before
	public void setup() throws Exception {
		LOGGER.log(Level.INFO, "Setting up test environment, clearing data");
		utx.begin();
	    em.joinTransaction();
		em.createQuery("delete from Record").executeUpdate();
	    utx.commit();
	}
	
	@After
	public void tearDown() {
		LOGGER.log(Level.INFO, "clearing environment");
	}
	
	@Test
	public void createRecord() {
		String key = "1";
		String metadataPrefix = "oai_dc";
		long lastModificationDate = System.currentTimeMillis();
		String xml = "<oai_dc></oai_dc>";
		Record record1 = oai.createRecord(key, metadataPrefix, lastModificationDate, xml);
		try {
			Record recordRead1 = oai.readRecord(key);
			assertTrue(record1.equals(recordRead1));
		} catch (RecordNotFoundException e) {
			fail(e.getMessage());
		}
		
	}

}
