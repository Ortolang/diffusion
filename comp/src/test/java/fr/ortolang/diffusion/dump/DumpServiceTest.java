package fr.ortolang.diffusion.dump;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

@RunWith(Arquillian.class)
public class DumpServiceTest {
    
    private static final Logger LOGGER = Logger.getLogger(DumpServiceTest.class.getName());

    @PersistenceContext
    private EntityManager em;
    
    @Resource(name="java:jboss/UserTransaction")
    private UserTransaction utx;
    
    @EJB
    private CoreService core;
    @EJB
    private MembershipService membership;
    @EJB
    private DumpService dump;
    
    @Deployment
    public static EnterpriseArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "importexport.jar");
        jar.addPackage("fr.ortolang.diffusion");
        jar.addPackage("fr.ortolang.diffusion.browser");
        jar.addPackage("fr.ortolang.diffusion.core");
        jar.addPackage("fr.ortolang.diffusion.core.entity");
        jar.addPackage("fr.ortolang.diffusion.core.wrapper");
        jar.addPackage("fr.ortolang.diffusion.event");
        jar.addPackage("fr.ortolang.diffusion.event.entity");
        jar.addPackage("fr.ortolang.diffusion.dump");
        jar.addClass("fr.ortolang.diffusion.extraction.ExtractionService");
        jar.addClass("fr.ortolang.diffusion.extraction.ExtractionServiceBean");
        jar.addClass("fr.ortolang.diffusion.extraction.ExtractionServiceException");
        jar.addPackage("fr.ortolang.diffusion.indexing");
        jar.addPackage("fr.ortolang.diffusion.membership");
        jar.addPackage("fr.ortolang.diffusion.membership.entity");
        jar.addPackage("fr.ortolang.diffusion.message");
        jar.addPackage("fr.ortolang.diffusion.message.entity");
        jar.addPackage("fr.ortolang.diffusion.notification");
        jar.addPackage("fr.ortolang.diffusion.registry");
        jar.addPackage("fr.ortolang.diffusion.registry.entity");
        jar.addPackage("fr.ortolang.diffusion.security");
        jar.addPackage("fr.ortolang.diffusion.security.authentication");
        jar.addPackage("fr.ortolang.diffusion.security.authorisation");
        jar.addPackage("fr.ortolang.diffusion.security.authorisation.entity");
        jar.addPackage("fr.ortolang.diffusion.store.binary");
        jar.addPackage("fr.ortolang.diffusion.store.binary.hash");
        jar.addPackage("fr.ortolang.diffusion.store.handle");
        jar.addPackage("fr.ortolang.diffusion.store.handle.entity");
        jar.addClass("fr.ortolang.diffusion.store.json.IndexableJsonContent");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexablePlainTextContent");
        jar.addPackage("fr.ortolang.diffusion.template");
        jar.addAsResource("config.properties");
        jar.addAsResource("schema/ortolang-item-schema.json");
        jar.addAsResource("schema/ortolang-workspace-schema.json");
        jar.addAsResource("json/meta.json");
        jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "diffusion-server-ear.ear");
        ear.addAsModule(jar);
        ear.addAsLibraries(pom.resolve("org.wildfly:wildfly-ejb-client-bom:pom:9.0.1.Final").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.codehaus.jettison:jettison:1.3.3").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("commons-io:commons-io:2.5").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-core:6.1.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-highlighter:6.1.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-analyzers-common:6.1.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-queryparser:6.1.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.tika:tika-core:1.13").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.tika:tika-parsers:1.13").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("commons-codec:commons-codec:1.10").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("com.github.fge:json-schema-validator:2.2.6").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.javers:javers-core:1.6.7").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.activiti:activiti-engine:5.21.0").withTransitivity().asFile());
        LOGGER.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

        return ear;
    }
 
    @Before
    public void setup() throws Exception {
        LOGGER.log(Level.INFO, "Setting up test environment,bootstrap base");
    }
    
    @After
    public void tearDown() {
        LOGGER.log(Level.INFO, "clearing environment");
    }

    @Test
    public void testLookup() throws Exception {
        final String WORKSPACE_KEY = "WS1";
        final String WORKSPACE_NAME = "Workspace1";
        final String WORKSPACE_NAME_UPDATE = "Workspace1.update";
        final String WORKSPACE_TYPE = "test";
        
        
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            core.createWorkspace(WORKSPACE_KEY, WORKSPACE_NAME, WORKSPACE_TYPE);
            Workspace ws = core.readWorkspace(WORKSPACE_KEY);

            assertEquals(WORKSPACE_KEY, ws.getKey());
            assertEquals(WORKSPACE_NAME, ws.getName());
            assertEquals(WORKSPACE_TYPE, ws.getType());
            assertTrue(ws.getHead().length() > 10);
            assertTrue(ws.getSnapshots().size() == 0);
            assertTrue(ws.getMembers().length() > 10);

            core.updateWorkspace(WORKSPACE_KEY, WORKSPACE_NAME_UPDATE);
            Workspace wsu = core.readWorkspace(WORKSPACE_KEY);

            assertEquals(WORKSPACE_KEY, wsu.getKey());
            assertEquals(WORKSPACE_NAME_UPDATE, wsu.getName());
            assertEquals(WORKSPACE_TYPE, wsu.getType());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dump.dump(WORKSPACE_KEY, baos, false);
            
            LOGGER.log(Level.INFO, "Export output: \r\n" + baos.toString("UTF-8"));

        } finally {
            loginContext.logout();
        }
    }
}