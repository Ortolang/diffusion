package fr.ortolang.diffusion.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@RunWith(Arquillian.class)
public class MembershipServiceTest {
	
	private static Logger logger = Logger.getLogger(MembershipServiceTest.class.getName());

	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private RegistryService registry;
	
	@ArquillianResource  
    InitialContext initialContext;  

	
	@Deployment
	public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "diffusion-server-ejb.jar");
		jar.addPackage("fr.ortolang.diffusion");
		// jar.addPackage("fr.ortolang.diffusion.bootstrap");
		jar.addPackage("fr.ortolang.diffusion.browser");
		jar.addPackage("fr.ortolang.diffusion.event");
		jar.addPackage("fr.ortolang.diffusion.event.entity");
		jar.addClass("fr.ortolang.diffusion.indexing.IndexingContext");
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
		jar.addAsResource("config.properties");
		jar.addAsResource("ontology/foaf.xml");
		jar.addAsResource("ontology/ortolang.xml");
		jar.addAsResource("ontology/ortolang-market.xml");
		jar.addAsResource("ontology/lexvo_2013-02-09.rdf");
		jar.addAsResource("ontology/lexvo-ontology.xml");
		jar.addAsResource("ontology/rdfs.xml");
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
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
	}

	@Before
	public void setup() throws MembershipServiceException, AccessDeniedException {
		logger.log(Level.INFO, "setting up test environment");
	}

	@After
	public void tearDown() {
		logger.log(Level.INFO, "clearing environment");
	}
	
	@Test
	public void testLogin() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("anonymous", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			String key = membership.getProfileKeyForConnectedIdentifier();
			assertEquals("anonymous", key);
		} finally {
			loginContext.logout();
		}
	}
		
	@Test
	public void testProfileInfos() throws LoginException, MembershipServiceException, AccessDeniedException {
		LoginContext loginContextRoot = UsernamePasswordLoginContextFactory.createLoginContext("root", "tagada54");
		loginContextRoot.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super", "User", "root@ortolang.org", ProfileStatus.ACTIVE);
			
			membership.createProfile("jmarple", "Jane", "Marple", "jmarple@ortolang.fr", ProfileStatus.ACTIVE);
			membership.createProfile("sholmes", "Sherlock", "Holmes", "sholmes@ortolang.fr", ProfileStatus.ACTIVE);logger.log(Level.FINE, "creating root profile");
			
			String key = membership.getProfileKeyForConnectedIdentifier();
			assertEquals("root", key);
		} catch ( ProfileAlreadyExistsException e ) {
			logger.log(Level.INFO, "Profile already exists");
		} 
		try {
			// Create infos
			membership.setProfileInfo("root", "presentation.prop1", "value1", ProfileDataVisibility.EVERYBODY.getValue(), ProfileDataType.STRING, "");
			membership.setProfileInfo("root", "presentation.prop2", "value2", ProfileDataVisibility.FRIENDS.getValue(), ProfileDataType.STRING, "");
			membership.setProfileInfo("root", "presentation.prop3", "value3", ProfileDataVisibility.NOBODY.getValue(), ProfileDataType.STRING, "");
			membership.setProfileInfo("root", "setting.prop4", "value4", ProfileDataVisibility.EVERYBODY.getValue(), ProfileDataType.STRING, "");
			membership.setProfileInfo("root", "setting.prop5", "value5", ProfileDataVisibility.NOBODY.getValue(), ProfileDataType.STRING, "");
			
			logger.log(Level.FINE, "creating friends group and adding sholmes");
			String friendGroupKey = membership.readProfile("root").getFriends();
			membership.createGroup(friendGroupKey, "root's friends", "List of collaborators of user root");
			membership.addMemberInGroup(friendGroupKey, "sholmes");
			Map<String, List<String>> friendsReadRules = new HashMap<String, List<String>>();
			friendsReadRules.put(friendGroupKey, Arrays.asList(new String[] { "read" }));
			authorisation.setPolicyRules(friendGroupKey, friendsReadRules);
			
			// Get infos with root profile
			logger.log(Level.INFO, "TEST1 : root should see all infos of his own profile.");
			List<ProfileData> infosSeenByRoot = membership.listProfileInfos("root", "");
			assertEquals(5, infosSeenByRoot.size());
			
			// Consult infos of category "presentation"
			logger.log(Level.INFO, "TEST2 : root should only see root's infos of category 'presentation'.");
			List<ProfileData> presentationSeenByRoot = membership.listProfileInfos("root", "presentation");
			assertEquals(3, presentationSeenByRoot.size());

			loginContextRoot.logout();
			
			LoginContext loginContextJMarple = UsernamePasswordLoginContextFactory.createLoginContext("jmarple", "jmarple");
			loginContextJMarple.login();
						
			// Get infos with jmarple profile
			logger.log(Level.INFO, "TEST3 : jmarple should only see root's infos with visibility set to EVERYBODY.");
			List<ProfileData> infosSeenByJMarple = membership.listProfileInfos("root", "");
			assertEquals(2, infosSeenByJMarple.size());
			
			// Consult infos of category "presentation"
			logger.log(Level.INFO, "TEST4 : jmarple should only see root's infos of category 'presentation' and with visibility set to EVERYBODY.");
			List<ProfileData> presentationSeenByJMarple = membership.listProfileInfos("root", "presentation");
			assertEquals(1, presentationSeenByJMarple.size());
			
			loginContextJMarple.logout();
			
			LoginContext loginContextSHolmes = UsernamePasswordLoginContextFactory.createLoginContext("sholmes", "sholmes");
			loginContextSHolmes.login();
			
			// Get infos with sholmes profile (friend of root)
			logger.log(Level.INFO, "TEST5 : sholmes should only see root's infos with visibility set to EVERYBODY OR FRIENDS.");
			List<ProfileData> infosSeenBySHolmes = membership.listProfileInfos("root", "");
			assertEquals(3, infosSeenBySHolmes.size());
			
			// Consult infos of category "presentation"
			logger.log(Level.INFO, "TEST6 : sholmes should only see root's infos of category 'presentation' and with visibility set to EVERYBODY or FRIENDS.");
			List<ProfileData> presentationSeenBySHolmes = membership.listProfileInfos("root", "presentation");
			assertEquals(2, presentationSeenBySHolmes.size());
			
			loginContextSHolmes.logout();						
			
		} catch ( MembershipServiceException | KeyNotFoundException | KeyAlreadyExistsException | AuthorisationServiceException e) {
			fail(e.getMessage());
		}
	}

}
