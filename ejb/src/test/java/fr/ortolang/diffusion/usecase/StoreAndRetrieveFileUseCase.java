package fr.ortolang.diffusion.usecase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.ObjectContainer;

@RunWith(Arquillian.class)
public class StoreAndRetrieveFileUseCase {

	private static Logger logger = Logger.getLogger(StoreAndRetrieveFileUseCase.class.getName());

	@EJB
	private BrowserService browser;

	@EJB
	private CoreService core;

	@Deployment
	public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.browser");
		jar.addPackage("fr.ortolang.diffusion.core");
		jar.addPackage("fr.ortolang.diffusion.core.entity");
		jar.addPackage("fr.ortolang.diffusion.registry");
		jar.addPackage("fr.ortolang.diffusion.store");
		jar.addPackage("fr.ortolang.diffusion.store.binary");
		jar.addPackage("fr.ortolang.diffusion.store.binary.hash");
		jar.addPackage("fr.ortolang.diffusion.usecase");
		jar.addAsResource("config.properties");
		jar.addAsResource("file1.jpg");
		logger.log(Level.INFO, "Created JAR for test : " + jar.toString(true));
		
		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
		ear.addAsModule(jar);
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("javax.activation:activation:1.1.1").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("commons-io:commons-io:2.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("com.healthmarketscience.rmiio:rmiio:2.0.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.wildfly:wildfly-ejb-client-bom:pom:8.0.0.Beta1").withTransitivity().asFile());
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));
		
		return ear;
	}

	@Test
	public void testHostSimpleFile() throws URISyntaxException {
		//Path origin = Paths.get(HostAndRetrieveFileTest.class.getClassLoader().getResource("file1.jpg").getPath());
		Path origin = Paths.get("/home/jerome/Images/test.jpg");
		logger.log(Level.INFO, "Origin file to insert in container : " + origin.toString());

		Path destination = Paths.get("/tmp/" + System.currentTimeMillis());
		logger.log(Level.INFO, "Destination file for retrieving content from container : " + destination.toString());

		String streamName = "file1";
		String key = UUID.randomUUID().toString();

		// Create the DiffusionObject
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Files.copy(origin, baos);
			core.createContainer(key, "A test container");
			core.addDataStreamToContainer(key, streamName, baos.toByteArray());
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Check that the object is registered in the browser
//		try {
//			DigitalObject object = browser.lookup(key);
//			assertEquals(object.getDiffusionObjectKey(), key);
//			assertEquals(object.getDiffusionObjectIdentifier().getService(), CoreService.SERVICE_NAME);
//			assertEquals(object.getDiffusionObjectIdentifier().getType(), ObjectContainer.OBJECT_TYPE);
//		} catch (BrowserServiceException | EntryNotFoundException e) {
//			logger.log(Level.SEVERE, e.getMessage(), e);
//			fail(e.getMessage());
//		}

		// Retrieve this object using the key
		try {
			ObjectContainer container = core.getContainer(key);
			String hash = container.getStreams().get(streamName);
			logger.log(Level.INFO, "stream has been stored in container with hash: " + hash);
			byte[] data = core.getDataStreamFromContainer(key, streamName);
			Files.copy(new ByteArrayInputStream(data), destination);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Compare origin and destination :
		try {
			InputStream input1 = Files.newInputStream(origin);
			InputStream input2 = Files.newInputStream(destination);
			assertTrue(IOUtils.contentEquals(input1, input2));
			input1.close();
			input2.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Delete destination
		try {
			Files.delete(destination);
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		try {
			Properties properties = new Properties();
			properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming.ejb");
			Context jndi = new InitialContext(properties);
			NamingEnumeration<NameClassPair> enumeration = jndi.list("");
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				logger.log(Level.INFO, "jndi entry : " + name);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

}
