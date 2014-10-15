package fr.ortolang.diffusion.runtime.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@RunWith(Arquillian.class)
public class RuntimeServiceTest {

	private static Logger logger = Logger.getLogger(RuntimeServiceTest.class.getName());

	@EJB
	private MembershipService membership;
	@EJB
	private RuntimeService runtime;
	
	@Deployment
	public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "diffusion-server-ejb.jar");
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.browser");
		jar.addPackage("fr.ortolang.diffusion.event");
		jar.addPackage("fr.ortolang.diffusion.event.entity");
		jar.addClass("fr.ortolang.diffusion.indexing.IndexingService");
		jar.addClass("fr.ortolang.diffusion.indexing.IndexingServiceBean");
		jar.addClass("fr.ortolang.diffusion.indexing.IndexingServiceException");
		jar.addPackage("fr.ortolang.diffusion.membership");
		jar.addPackage("fr.ortolang.diffusion.membership.entity");
		jar.addPackage("fr.ortolang.diffusion.notification");
		jar.addPackage("fr.ortolang.diffusion.registry");
		jar.addPackage("fr.ortolang.diffusion.registry.entity");
		jar.addPackage("fr.ortolang.diffusion.security.authentication");
		jar.addPackage("fr.ortolang.diffusion.security.authorisation");
		jar.addPackage("fr.ortolang.diffusion.security.authorisation.entity");
		jar.addPackage("fr.ortolang.diffusion.store.binary");
		jar.addPackage("fr.ortolang.diffusion.store.binary.hash");
		jar.addPackage("fr.ortolang.diffusion.store.index");
		jar.addPackage("fr.ortolang.diffusion.store.triple");
		jar.addPackage("fr.ortolang.diffusion.runtime");
		jar.addPackage("fr.ortolang.diffusion.runtime.activiti");
		jar.addPackage("fr.ortolang.diffusion.runtime.entity");
		jar.addPackage("fr.ortolang.diffusion.runtime.task");
		jar.addAsResource("config.properties");
		jar.addAsResource("ontology/foaf.xml");
		jar.addAsResource("ontology/ortolang.xml");
		jar.addAsResource("ontology/rdfs.xml");
		jar.addAsResource("activiti.cfg.xml");
		jar.addAsResource("workflows/HelloWorldProcess.bpmn");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
		logger.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "diffusion-server-ear.ear");
		ear.addAsModule(jar);
		ear.addAsLibraries(pom.resolve("commons-io:commons-io:2.4").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("com.healthmarketscience.rmiio:rmiio:2.0.4").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.wildfly:wildfly-ejb-client-bom:pom:8.0.0.Final").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.tika:tika-core:1.4").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-core:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-highlighter:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-analyzers-common:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-queryparser:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-repository-api:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-repository-sail:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-sail-api:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-sail-nativerdf:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-model:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-rio-api:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-rio-rdfxml:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-queryparser-sparql:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-queryresultio-api:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-queryresultio-sparqlxml:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.openrdf.sesame:sesame-queryresultio-sparqljson:2.7.8").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.velocity:velocity:1.7").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.apache.velocity:velocity-tools:2.0").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.activiti:activiti-engine:5.16.3").withTransitivity().asFile());
		ear.addAsLibraries(pom.resolve("org.springframework:spring-context:4.0.6.RELEASE").withTransitivity().asFile());
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
	}

	@Test
	public void testLogin() throws LoginException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			String key = membership.getProfileKeyForConnectedIdentifier();
			assertEquals("guest", key);
		} finally {
			loginContext.logout();
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateProcessDefinitionAsUnauthentifiedUser() throws LoginException, KeyAlreadyExistsException, AccessDeniedException, RuntimeServiceException, IOException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			runtime.createProcessDefinition("K1", new ByteArrayInputStream("".getBytes()));
			fail("Should have raised an AccessDeniedException");
		} finally {
			loginContext.logout();
		}
	}
	
	@Test
	public void testCreateProcessDefinitionAsRootUser() throws LoginException, KeyAlreadyExistsException, AccessDeniedException, RuntimeServiceException, IOException, KeyNotFoundException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("root", "tagada54");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			runtime.createProcessDefinition("K1", this.getClass().getClassLoader().getResourceAsStream("workflows/HelloWorldProcess.bpmn"));
			List<ProcessDefinition> defs = runtime.listProcessDefinitions();
			
			assertEquals(1, defs.size());
			assertEquals("K1", defs.get(0).getKey());
			
			ProcessDefinition def = runtime.readProcessDefinition("K1");
			
			assertEquals(1, def.getVersion());
			assertEquals("Hello World Process", def.getName());
		} finally {
			loginContext.logout();
		}
	}
	
	@Test
	public void testCreateProcessInstanceAsRootUser() throws LoginException, KeyAlreadyExistsException, AccessDeniedException, RuntimeServiceException, IOException, KeyNotFoundException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("root", "tagada54");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			runtime.createProcessDefinition("K2", this.getClass().getClassLoader().getResourceAsStream("workflows/HelloWorldProcess.bpmn"));
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("name", "junit");
			runtime.startProcessInstance("K3", "K2", params);
			
			ProcessInstance ins = runtime.readProcessInstance("K3");
			
			assertEquals("K3", ins.getKey());
			assertEquals("root", ins.getInitier());
		} finally {
			loginContext.logout();
		}
	}
	
}