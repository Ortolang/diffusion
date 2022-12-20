package fr.ortolang.diffusion.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.IOUtils;
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

import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;
import fr.ortolang.diffusion.archive.format.FichMetaConstants;
import fr.ortolang.diffusion.core.AliasAlreadyExistsException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathAlreadyExistsException;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;
import fr.ortolang.diffusion.core.WorkspaceUnchangedException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@RunWith(Arquillian.class)
public class ArchiveServiceTest {
    
    private static final Logger LOGGER = Logger.getLogger(ArchiveServiceTest.class.getName());

    @EJB
    private ArchiveService archive;

    @EJB
    private CoreService core;

    @EJB
    private MembershipService membership;

    @EJB
    private BinaryStoreService binary;
    
    @Deployment
    public static EnterpriseArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "diffusion-server-ejb.jar");
        jar.addPackage("fr.ortolang.diffusion");
        jar.addPackage("fr.ortolang.diffusion.browser");
        jar.addPackage("fr.ortolang.diffusion.core");
        jar.addPackage("fr.ortolang.diffusion.core.entity");
        jar.addPackage("fr.ortolang.diffusion.core.wrapper");
        jar.addPackage("fr.ortolang.diffusion.event");
        jar.addPackage("fr.ortolang.diffusion.event.entity");
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
        jar.addClass("fr.ortolang.diffusion.indexing.IndexingService");
        jar.addClass("fr.ortolang.diffusion.indexing.IndexingServiceException");
        jar.addClass("fr.ortolang.diffusion.indexing.OrtolangIndexableContent");
        jar.addPackage("fr.ortolang.diffusion.template");
        jar.addPackage("fr.ortolang.diffusion.archive");
        jar.addPackage("fr.ortolang.diffusion.archive.facile");
        jar.addPackage("fr.ortolang.diffusion.archive.facile.entity");
        jar.addPackage("fr.ortolang.diffusion.archive.exception");
        jar.addPackage("fr.ortolang.diffusion.archive.aip.entity");
        jar.addPackage("fr.ortolang.diffusion.archive.format");
        jar.addPackage("fr.ortolang.diffusion.util");
        jar.addPackage("fr.ortolang.diffusion.oai.exception");
        jar.addPackage("fr.ortolang.diffusion.oai.format.builder");
        jar.addPackage("fr.ortolang.diffusion.jobs");
        jar.addPackage("fr/ortolang/diffusion/jobs/entity");
        jar.addAsResource("config.properties");
        jar.addAsResource("schema/system-aip-schema.json");
        jar.addAsResource("schema/system-fichmeta-schema.json");
        jar.addAsResource("schema/ortolang-workspace-schema.json");
        jar.addAsResource("json/meta.json");
        jar.addAsResource("archive/aip_test_sample.xml");
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
        ear.addAsLibraries(pom.resolve("org.apache.commons:commons-compress:1.21").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.codehaus.woodstox:stax2-api:4.2.1").withTransitivity().asFile());
        LOGGER.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

        return ear;
    }

    @Before
    public void setup() {
        LOGGER.log(Level.INFO, "setting up test environment");
    }

    @After
    public void tearDown() {
        LOGGER.log(Level.INFO, "clearing environment");
    }

    @Test
    public void testStoreAip() throws IOException, ArchiveServiceException, LoginException, AccessDeniedException, MembershipServiceException, CoreServiceException, KeyAlreadyExistsException, AliasAlreadyExistsException, WorkspaceReadOnlyException, KeyNotFoundException, WorkspaceUnchangedException, DataCollisionException, BinaryStoreServiceException, DataNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException {
        LOGGER.log(Level.INFO, "Testing storeAip method from ArchiveService");
        String rootCollection = null;
        String fileDataobject = null;
        LOGGER.log(Level.INFO, "Creating a workspace for ArchiveServiceTest");
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            Workspace ws = core.createWorkspace("K1", "Blabla2", "test");
            rootCollection = ws.getHead();
            
            String hash1 = core.put(new ByteArrayInputStream("First version of fileone.txt".getBytes()));
            DataObject objectv1 = core.createDataObject(ws.getKey(), "/fileone.txt", hash1);
            fileDataobject = objectv1.getKey();

            InputStream schemaWorkspaceInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-workspace-schema.json");
            String schemaWorkspaceHash = core.put(schemaWorkspaceInputStream);
            core.createMetadataFormat(MetadataFormat.WORKSPACE, "Les métadonnées associées à un espace de travail.", schemaWorkspaceHash, "", true, true);
            core.snapshotWorkspace(ws.getKey());
        } finally {
            loginContext.logout();
        }

        loginContext = UsernamePasswordLoginContextFactory.createLoginContext("root", "tagada54");
        loginContext.login();
        try {
            InputStream schemaAipInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-aip-schema.json");
            String schemaAipHash = core.put(schemaAipInputStream);
            core.createMetadataFormat(MetadataFormat.AIP, "Les métadonnées correspondant au fichier aip.xml.", schemaAipHash, "", false, false);
            InputStream schemaFichmetaInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-fichmeta-schema.json");
            String schemaFichmetaHash = core.put(schemaFichmetaInputStream);
            core.createMetadataFormat(MetadataFormat.FICHMETA, "Les métadonnées correspondant à une section fichmeta dans le fichier aip.xml.", schemaFichmetaHash, "", false, false);

            try {
                archive.storeAip("fesfes");
                fail("Should throw ArchiveServiceException");
            } catch (ArchiveServiceException e) {}

            LOGGER.log(Level.INFO, "Loading a sample of aip.xml");
            InputStream aipInputStream = getClass().getClassLoader().getResourceAsStream("archive/aip_test_sample.xml");
            String aipXml = IOUtils.toString(aipInputStream, StandardCharsets.UTF_8);
            aipXml = aipXml.replaceFirst("#root#", rootCollection);
            archive.storeAip(aipXml);

            // Checks metadata created
            List<String> mds = core.systemFindMetadataObjectsForTargetAndName(rootCollection, MetadataFormat.AIP);
            assertEquals(1, mds.size());

            MetadataObject aipMetadata = core.systemReadMetadataObject(mds.get(0));
            InputStream aipMetadataInputStream = binary.get(aipMetadata.getStream());
            String aipMetadataString = IOUtils.toString(aipMetadataInputStream, StandardCharsets.UTF_8);
            JsonObject jsonObject = Json.createReader(new StringReader(aipMetadataString)).readObject();
            assertNotNull(jsonObject.get("identifier"));
            assertEquals("ark:/87895/1.19-1519937", jsonObject.getString("identifier").toString());

            mds = core.systemFindMetadataObjectsForTargetAndName(fileDataobject, MetadataFormat.FICHMETA);
            assertEquals(1, mds.size());

            MetadataObject fichmetaMetadata = core.systemReadMetadataObject(mds.get(0));   
            InputStream fichmetaMetadataInputStream = binary.get(fichmetaMetadata.getStream());
            String fichmetaMetadataString = IOUtils.toString(fichmetaMetadataInputStream, StandardCharsets.UTF_8);       
            JsonObject jsonFichmetaObject = Json.createReader(new StringReader(fichmetaMetadataString)).readObject();
            assertNotNull(jsonFichmetaObject.get(FichMetaConstants.IDFICHIER));
            assertEquals("ark:/87895/1.19-1519937/164", jsonFichmetaObject.getString(FichMetaConstants.IDFICHIER).toString());
        } finally {
            loginContext.logout();
        }
    }
}
