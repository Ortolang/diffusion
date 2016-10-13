package fr.ortolang.diffusion.core;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@RunWith(Arquillian.class)
public class CoreServiceTest {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceTest.class.getName());

    @EJB
    private MembershipService membership;
    @EJB
    private CoreService core;
    @EJB
    private RegistryService registry;

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
        jar.addClass("fr.ortolang.diffusion.store.json.IndexableJsonContent");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexablePlainTextContent");
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
    public void testLogin() throws LoginException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("anonymous", "password");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            String key = membership.getProfileKeyForConnectedIdentifier();
            assertEquals("anonymous", key);
        } finally {
            loginContext.logout();
        }
    }

    @Test(expected = AccessDeniedException.class)
    public void testCreateWorkspaceAsUnauthentifiedUser()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, AliasAlreadyExistsException {
        LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
        try {
            membership.createProfile("Anonymous", "", "anonymous@ortolang.fr");
        } catch (ProfileAlreadyExistsException e) {
            LOGGER.log(Level.INFO, "Profile anonymous already exists !!");
        }
        core.createWorkspace("K1", "alias", "Blabla", "test");
        fail("Should have raised an AccessDeniedException");
    }

    @Test(expected = KeyAlreadyExistsException.class)
    public void testCreateWorkspaceWithExistingKey()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, AliasAlreadyExistsException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            core.createWorkspace("K2", "Blabla2", "test");
            core.createWorkspace("K2", "Blabla3", "test");
            fail("Should have raised a KeyAlreadyExistsException");
        } finally {
            loginContext.logout();
        }
    }

    @Test(expected = AliasAlreadyExistsException.class)
    public void testCreateWorkspaceWithExistingAlias()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, AliasAlreadyExistsException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            core.createWorkspace("WSK1", "workspace-001", "Blabla2", "test");
            core.createWorkspace("WSK2", "workspace-001", "Blabla2", "test");
            fail("Should have raised a AliasAlreadyExistsException");
        } finally {
            loginContext.logout();
        }
    }

    @Test(expected = KeyNotFoundException.class)
    public void testReadUnexistingWorkspace() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException,
            KeyNotFoundException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            core.readWorkspace("UNEXISTING");
            fail("Should have raised a KeyNotFoundException");
        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testReadUnreadableWorkspace()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, AliasAlreadyExistsException {
        final String WORKSPACE_KEY = "WSP";
        final String WORKSPACE_NAME = "WorkspaceProtected";
        final String WORKSPACE_TYPE = "test";

        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            core.createWorkspace(WORKSPACE_KEY, WORKSPACE_NAME, WORKSPACE_TYPE);
        } finally {
            loginContext.logout();
        }

        loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user2", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "TWO", "user.two@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user2 already exists !!");
            }
            core.readWorkspace(WORKSPACE_KEY);
        } catch ( AccessDeniedException e ) {
            fail("workspaces should be readable by everybody");
        } finally {
            loginContext.logout();
        }

    }

    @Test
    public void testCRUDWorkspace()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, WorkspaceReadOnlyException,
            AliasAlreadyExistsException {
        final String WORKSPACE_KEY = "WS1";
        final String WORKSPACE_NAME = "Workspace1";
        final String WORKSPACE_NAME_UPDATE = "Workspace1.update";
        final String WORKSPACE_TYPE = "test";

        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
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

            core.deleteWorkspace(WORKSPACE_KEY);

            try {
                core.readWorkspace(WORKSPACE_KEY);
            } catch (KeyNotFoundException e) {
                //
            }

        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testMetadataFormat()
            throws LoginException, MembershipServiceException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, DataCollisionException, KeyNotFoundException,
            InvalidPathException, MetadataFormatException, PathNotFoundException, WorkspaceReadOnlyException, AliasAlreadyExistsException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }

            int mdFormatNbr = core.listMetadataFormat().size();

            InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-item-schema.json");
            String schemaHash = core.put(schemaInputStream);
            String id = core.createMetadataFormat("ortolang-item-json", "ORTOLANG Item", schemaHash, null, true, true);

            List<MetadataFormat> mfs = core.listMetadataFormat();
            assertEquals(mdFormatNbr + 1, mfs.size());

            MetadataFormat mf = core.getMetadataFormat("ortolang-item-json");
            assertEquals("ortolang-item-json", mf.getName());
            assertEquals("ORTOLANG Item", mf.getDescription());
            assertEquals(schemaHash, mf.getSchema());

            MetadataFormat mfid = core.findMetadataFormatById(id);
            assertEquals("ortolang-item-json", mfid.getName());

            String wsk = UUID.randomUUID().toString();
            core.createWorkspace(wsk, "WorkspaceCollection", "test");

            String metak = UUID.randomUUID().toString();
            InputStream metadataInputStream = getClass().getClassLoader().getResourceAsStream("json/meta.json");
            String metadataHash = core.put(metadataInputStream);
            core.createMetadataObject(wsk, metak, "/", mf.getName(), metadataHash, "meta.json", false);
            MetadataObject metadata = core.readMetadataObject(metak);
            assertEquals("ortolang-item-json:1", metadata.getFormat());

        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testCRUDCollection()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, InvalidPathException,
            CollectionNotEmptyException, DataCollisionException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException, AliasAlreadyExistsException, RegistryServiceException,
            WorkspaceUnchangedException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }

            String wsk = UUID.randomUUID().toString();
            core.createWorkspace(wsk, "WorkspaceCollection", "test");

            core.createCollection(wsk, "/a");
            core.createCollection(wsk, "/a/b");
            core.createCollection(wsk, "/a/c");

            LOGGER.log(Level.INFO, "Workspace created with 3 collections");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            String akey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a");
            Collection collection = core.readCollection(akey);
            assertEquals(1, collection.getClock());
            assertEquals("a", collection.getName());
            assertEquals(1, collection.getClock());
            assertEquals(2, collection.getElements().size());
            String bkey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a/b");
            String ckey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a/c");
            assertTrue(collection.findElementByName("b").getKey().equals(bkey));
            assertTrue(collection.findElementByName("c").getKey().equals(ckey));

            core.deleteCollection(wsk, "/a/b");
            collection = core.readCollection(akey);
            assertEquals(1, collection.getClock());
            assertEquals(1, collection.getElements().size());
            assertTrue(collection.findElementByName("b") == null);
            assertTrue(collection.findElementByName("c").getKey().equals(ckey));

            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            InputStream schemaWorkspaceInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-workspace-schema.json");
            String schemaWorkspaceHash = core.put(schemaWorkspaceInputStream);
            core.createMetadataFormat(MetadataFormat.WORKSPACE, "Les métadonnées associées à un espace de travail.", schemaWorkspaceHash, "", true, true);

            Workspace workspace = core.readWorkspace(wsk);
            assertEquals(0, workspace.getSnapshots().size());
            assertEquals(1, workspace.getClock());
            assertTrue(workspace.hasChanged());
            core.snapshotWorkspace(wsk);
            workspace = core.readWorkspace(wsk);
            assertEquals(1, workspace.getSnapshots().size());
            assertEquals(2, workspace.getClock());
            assertFalse(workspace.hasChanged());

            LOGGER.log(Level.INFO, "Workspace snapshotted");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            try {
                core.snapshotWorkspace(wsk);
                fail("A second snapshot without changes should produce an exception");
            } catch (WorkspaceUnchangedException e) {
                LOGGER.log(Level.INFO, e.getMessage());
            } catch (Exception e) {
                fail("Should produce WorkspaceUnchangedException");
            }

            core.createCollection(wsk, "/a/d");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            core.snapshotWorkspace(wsk);
            workspace = core.readWorkspace(wsk);
            assertEquals(2, workspace.getSnapshots().size());
            assertEquals(3, workspace.getClock());
            assertFalse(workspace.hasChanged());

            core.deleteCollection(wsk, "/a/d");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            core.createCollection(wsk, "/a/e");
            core.snapshotWorkspace(wsk);
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            core.createCollection(wsk, "/a/c/f");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            String acfkey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a/c/f");
            int acfclock = core.readCollection(acfkey).getClock();
            core.moveCollection(wsk, "/a/c/f", "/a/e/f");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));
            collection = core.readCollection(acfkey);
            assertEquals(acfclock, collection.getClock());
            assertEquals("f", collection.getName());

            core.snapshotWorkspace(wsk);
            core.createCollection(wsk, "/a/e/f/h");
            acfclock = core.readCollection(acfkey).getClock();
            core.moveCollection(wsk, "/a/e/f", "/a/c/g");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));
            acfkey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a/c/g");
            collection = core.readCollection(acfkey);
            assertEquals(1, collection.getElements().size());
            assertEquals(acfclock + 1, collection.getClock());
            assertEquals("g", collection.getName());

            core.snapshotWorkspace(wsk);
            core.createCollection(wsk, "/a/c/g/d1");
            core.createCollection(wsk, "/a/c/g/d1/d2");
            core.createCollection(wsk, "/a/c/g/d1/d2/d3");
            try {
                core.deleteCollection(wsk, "/a/c/g");
                fail("collection is not empty and should have raised an exception");
            } catch ( CollectionNotEmptyException e ) {
                //
            }
            core.deleteCollection(wsk, "/a/c/g", true);
            
            //Wrong moves
            try {
                core.moveCollection(wsk, "/a/c", "/a/c");
                fail("move into the same path should raise an exception");
            } catch ( InvalidPathException e ) {
                //
            }
            try {
                core.moveCollection(wsk, "/a", "/a/e");
                fail("move into a children path should raise an exception");
            } catch ( InvalidPathException e ) {
                //
            }

            // Bulk move
            core.snapshotWorkspace(wsk);
            core.createCollection(wsk, "/m");
            core.createCollection(wsk, "/m/a");
            core.createCollection(wsk, "/m/b");
            core.createCollection(wsk, "/n");
            core.createCollection(wsk, "/n/a");
            core.createCollection(wsk, "/n/b");
            core.createCollection(wsk, "/o");
            List<String> sources = Arrays.asList("/m/a", "/n");
            try {
                core.moveElements(wsk, sources, "/o");
                fail("trying to move elements from different collections; should have raised an exception");
            } catch (InvalidPathException e) {
                //
            }
            sources = Arrays.asList("/m/a", "/m/b");
            try {
                core.moveElements(wsk, sources, "/n");
                fail("trying to move elements with one already existing in destination; should have raised an exception");
            } catch (PathAlreadyExistsException e){
                //
            }
            String oKey = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/o");
            core.moveElements(wsk, sources, "/o");
            Collection collectionO = core.readCollection(oKey);
            assertNotNull(collectionO.findElementByName("a"));
            assertNotNull(collectionO.findElementByName("b"));

            // Bulk delete
            sources = Arrays.asList("/o/a", "/n");
            try {
                core.deleteElements(wsk, sources, false);
                fail("trying to delete elements from different collections; should have raised an exception");
            } catch (InvalidPathException e) {
                //
            }
            sources = Arrays.asList("/o", "/n");
            try {
                core.deleteElements(wsk, sources, false);
                fail("trying to delete non-empty collections; should have raised an exception");
            } catch (CollectionNotEmptyException e) {
                //
            }
            sources = Arrays.asList("/o", "/n");
            core.deleteElements(wsk, sources, true);
            String head = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/");
            Collection collectionHead = core.readCollection(head);
            assertNull(collectionHead.findElementByName("o"));
            assertNull(collectionHead.findElementByName("n"));

            LOGGER.log(Level.INFO, walkWorkspace(wsk));

        } finally {
            loginContext.logout();
        }
    }
    
    @Test
    public void testUpdateDataObjectWithSnapshot()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, InvalidPathException,
            CollectionNotEmptyException, DataCollisionException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException, AliasAlreadyExistsException, RegistryServiceException,
            WorkspaceUnchangedException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }

            String wskey = UUID.randomUUID().toString();
            core.createWorkspace(wskey, "WorkspaceCollection", "test");

            core.createCollection(wskey, "/a");
            core.createCollection(wskey, "/a/b");
            String hash1 = core.put(new ByteArrayInputStream("First version of fileone.txt".getBytes()));
            DataObject objectv1 = core.createDataObject(wskey, "/a/b/fileone.txt", hash1);
            
            LOGGER.log(Level.INFO, "Workspace created with 2 collections and one dataobject");        
            LOGGER.log(Level.INFO, walkWorkspace(wskey));
                   
            String bkeyv1 = core.resolveWorkspacePath(wskey, Workspace.HEAD, "/a/b");
            String okeyv1 = core.resolveWorkspacePath(wskey, Workspace.HEAD, "/a/b/fileone.txt");
            Collection collectionv1 = core.readCollection(bkeyv1);
            CollectionElement elementv1 = collectionv1.findElementByName("fileone.txt");
            DataObject dataobjectv1 = core.readDataObject(okeyv1);
            
            assertEquals(okeyv1, objectv1.getKey());
            assertEquals(hash1, objectv1.getStream());
            assertEquals(28, objectv1.getSize());
            assertEquals("text/plain", objectv1.getMimeType());
            
            assertEquals(okeyv1, dataobjectv1.getKey());
            assertEquals(hash1, dataobjectv1.getStream());
            assertEquals(28, dataobjectv1.getSize());
            assertEquals("text/plain", dataobjectv1.getMimeType());
            
            assertEquals(okeyv1, elementv1.getKey());
            assertEquals("text/plain", elementv1.getMimeType());
            assertEquals(28, elementv1.getSize());
            
            core.snapshotWorkspace(wskey);
            String hash2 = core.put(new ByteArrayInputStream("Second version of fileone.txt".getBytes()));
            DataObject objectv2 = core.updateDataObject(wskey, "/a/b/fileone.txt", hash2);
            
            LOGGER.log(Level.INFO, "Workspace snapshotted and object updated");
            LOGGER.log(Level.INFO, walkWorkspace(wskey));
            
            String bkeyv2 = core.resolveWorkspacePath(wskey, Workspace.HEAD, "/a/b");
            String okeyv2 = core.resolveWorkspacePath(wskey, Workspace.HEAD, "/a/b/fileone.txt");
            Collection collectionv2 = core.readCollection(bkeyv2);
            CollectionElement elementv2 = collectionv2.findElementByName("fileone.txt");
            DataObject dataobjectv2 = core.readDataObject(okeyv2);
            
            assertEquals(okeyv2, objectv2.getKey());
            assertEquals(hash2, objectv2.getStream());
            assertEquals(29, objectv2.getSize());
            assertEquals("text/plain", objectv2.getMimeType());
            
            assertEquals(okeyv2, dataobjectv2.getKey());
            assertEquals(hash2, dataobjectv2.getStream());
            assertEquals(29, dataobjectv2.getSize());
            assertEquals("text/plain", dataobjectv2.getMimeType());
            
            assertEquals(okeyv2, elementv2.getKey());
            assertEquals("text/plain", elementv2.getMimeType());
            assertEquals(29, elementv2.getSize());
            
            bkeyv1 = core.resolveWorkspacePath(wskey, "1", "/a/b");
            okeyv1 = core.resolveWorkspacePath(wskey, "1", "/a/b/fileone.txt");
            collectionv1 = core.readCollection(bkeyv1);
            elementv1 = collectionv1.findElementByName("fileone.txt");
            dataobjectv1 = core.readDataObject(okeyv1);
            
            assertEquals(okeyv1, objectv1.getKey());
            assertEquals(hash1, objectv1.getStream());
            assertEquals(28, objectv1.getSize());
            assertEquals("text/plain", objectv1.getMimeType());
            
            assertEquals(okeyv1, dataobjectv1.getKey());
            assertEquals(hash1, dataobjectv1.getStream());
            assertEquals(28, dataobjectv1.getSize());
            assertEquals("text/plain", dataobjectv1.getMimeType());
            
            assertEquals(okeyv1, elementv1.getKey());
            assertEquals("text/plain", elementv1.getMimeType());
            assertEquals(28, elementv1.getSize());
            

        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testDeleteCollectionElementConcurrently()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException,
            AliasAlreadyExistsException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }

            String wsk = UUID.randomUUID().toString();
            core.createWorkspace(wsk, "WorkspaceCollection", "test");

            core.createCollection(wsk, "/a");
            core.createCollection(wsk, "/a/a");
            core.createCollection(wsk, "/a/b");
            core.createCollection(wsk, "/a/c");
            core.createCollection(wsk, "/a/d");
            core.createCollection(wsk, "/a/e");
            core.createCollection(wsk, "/a/f");
            core.createCollection(wsk, "/a/g");
            core.createCollection(wsk, "/a/h");
            core.createCollection(wsk, "/a/i");
            core.createCollection(wsk, "/a/j");

            LOGGER.log(Level.INFO, "Workspace created");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            String key = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a");
            Collection col = core.readCollection(key);
            int initialSize = col.getElements().size();

            List<CollectionElementDeleter> deleters = new ArrayList<CollectionElementDeleter> ();
            deleters.add(new CollectionElementDeleter(wsk, "/a/a"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/b"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/c"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/d"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/e"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/f"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/g"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/h"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/i"));
            deleters.add(new CollectionElementDeleter(wsk, "/a/j"));

            ThreadGroup group = new ThreadGroup("Deleters-ThreadGroup");
            List<Thread> threads = new ArrayList<Thread>();
            for (CollectionElementDeleter deleter : deleters) {
                threads.add(new Thread(group, deleter));
            }
            for (Thread thread : threads) {
                thread.start();
            }

            while (group.activeCount() > 0) {
                LOGGER.log(Level.INFO, "wait for threads to finish");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.INFO, "interrupted", e);
                }
            }

            int expectedSize = initialSize;
            for (CollectionElementDeleter deleter : deleters) {
                if ( deleter.hasBeenDeleted() ) {
                    expectedSize--;
                }
            }

            key = core.resolveWorkspacePath(wsk, Workspace.HEAD, "/a");
            col = core.readCollection(key);

            assertEquals(expectedSize, col.getElements().size());

        } finally {
            loginContext.logout();
        }
    }
    
    @Test
    public void testCRUDMetadata()
            throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException, InvalidPathException,
            CollectionNotEmptyException, DataCollisionException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException, AliasAlreadyExistsException, RegistryServiceException,
            MetadataFormatException, WorkspaceUnchangedException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            
            String sha1 = core.put(new ByteArrayInputStream("dadaduc1".getBytes()));
            String sha11 = core.put(new ByteArrayInputStream("dadaduc11".getBytes()));
            String sha111 = core.put(new ByteArrayInputStream("dadaduc111".getBytes()));
            String sha2 = core.put(new ByteArrayInputStream("dadaduc2".getBytes()));
            String sha3 = core.put(new ByteArrayInputStream("dadaduc3".getBytes()));
            String sha4 = core.put(new ByteArrayInputStream("dadaduc4".getBytes()));
            String sha5 = core.put(new ByteArrayInputStream("dadaduc5".getBytes()));
            String sha55 = core.put(new ByteArrayInputStream("dadaduc55".getBytes()));

            core.createMetadataFormat("acl", "ACL", null, null, false, false);
            core.createMetadataFormat("foo", "FOO", null, null, false, false);

            String wsk = UUID.randomUUID().toString();
            core.createWorkspace(wsk, "WorkspaceCollection", "test");

            core.createCollection(wsk, "/a");
            core.createCollection(wsk, "/a/b");
            core.createCollection(wsk, "/a/c");
            core.createCollection(wsk, "/a/d");
            core.createMetadataObject(wsk, "/a", "acl", sha1, "acl.json", false);
            core.createMetadataObject(wsk, "/a/b", "acl", sha2, "acl.json", false);
            core.createMetadataObject(wsk, "/a/c", "acl", sha3, "acl.json", false);
            core.createMetadataObject(wsk, "/", "acl", sha4, "acl.json", false);
            core.createMetadataObject(wsk, "/a/d", "acl", sha5, "acl.json", false);
            core.createMetadataObject(wsk, "/a/d", "foo", sha55, "foo.bar", false);
            LOGGER.log(Level.INFO, "Workspace created with 4 collections and 5 metadata");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            String akey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a", "acl");
            MetadataObject md1 = core.readMetadataObject(akey);
            assertEquals("acl", md1.getName());
            assertEquals(sha1, md1.getStream());
            
            String abkey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/b", "acl");
            MetadataObject md2 = core.readMetadataObject(abkey);
            assertEquals("acl", md2.getName());
            assertEquals(sha2, md2.getStream());
            
            String ackey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/c", "acl");
            MetadataObject md3 = core.readMetadataObject(ackey);
            assertEquals("acl", md3.getName());
            assertEquals(sha3, md3.getStream());

            String adkey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/", "acl");
            MetadataObject md4 = core.readMetadataObject(adkey);
            assertEquals("acl", md4.getName());
            assertEquals(sha4, md4.getStream());

            String aekey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/d", "acl");
            MetadataObject md5 = core.readMetadataObject(aekey);
            assertEquals("acl", md5.getName());
            assertEquals(sha5, md5.getStream());
            String aeekey = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/d", "foo");
            MetadataObject md55 = core.readMetadataObject(aeekey);
            assertEquals("foo", md55.getName());
            assertEquals(sha55, md55.getStream());

            core.snapshotWorkspace(wsk);
            LOGGER.log(Level.INFO, "Workspace snapshoted");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            // DELETE
            core.deleteMetadataObject(wsk, "/a/b", "acl", false);
            try {
                core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/b", "acl");
                fail("Should have raised an CoreServiceException");
            } catch (CoreServiceException e) {
                LOGGER.log(Level.INFO, "/a/b metadata ACL deleted");
            }

            core.deleteMetadataObject(wsk, "/", "acl", false);

            try {
                core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/", "acl");
                fail("Should have raised an CoreServiceException");
            } catch (CoreServiceException e) {
                LOGGER.log(Level.INFO, "/ metadata ACL deleted");
            }

            // UPDATE
            LOGGER.log(Level.INFO, walkWorkspace(wsk));
            LOGGER.log(Level.INFO, "Metadata update");
            core.updateMetadataObject(wsk, "/a", "acl", sha11, "acl.json", false);

            core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/d", "acl");
            LOGGER.log(Level.INFO, "/a/d metadata ACL still existing");

            LOGGER.log(Level.INFO, "Metadata update (purging children)");
            core.updateMetadataObject(wsk, "/a", "acl", sha111, "acl.json", true);

            try {
                core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/d", "acl");
                fail("Should have raised an CoreServiceException");
            } catch (CoreServiceException e) {
                LOGGER.log(Level.INFO, "/a/d metadata ACL deleted");
            }
            // Check other metadata still there
            core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/d", "foo");

            try {
                String ackey2 = core.resolveWorkspaceMetadata(wsk, Workspace.HEAD, "/a/c", "acl");
                registry.lookup(ackey2);
                LOGGER.log(Level.INFO, "/a/c metadata ACL [" + ackey2 + "] lookup OK.");
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "/a/c metadata ACL lookup FAILED: " + e.getMessage());
            }

            LOGGER.log(Level.INFO, "Metadata /a[acl] updated");
            LOGGER.log(Level.INFO, walkWorkspace(wsk));

            
        } finally {
            loginContext.logout();
        }
    }

    public class CollectionElementDeleter implements Runnable {

        private String wskey;
        private String path;
        private boolean deleted;

        private CollectionElementDeleter(String wskey, String path) {
            this.wskey = wskey;
            this.path = path;
            deleted = false;
        }

        public boolean hasBeenDeleted() {
            return deleted;
        }

        public void run() {
            try {
                LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
                loginContext.login();
                try {
                    LOGGER.log(Level.INFO, "Deleting collection at path: " + path);
                    core.deleteCollection(wskey, path);
                    deleted = true;
                } catch (CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | CollectionNotEmptyException | PathNotFoundException | WorkspaceReadOnlyException e) {
                    LOGGER.log(Level.SEVERE, "error during deleting collection: " + e.getMessage());
                } finally {
                    loginContext.logout();
                }
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login", e);
            }
        }
    }

    private String walkWorkspace(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        StringBuffer buffer = new StringBuffer();
        Workspace workspace = core.readWorkspace(key);
        buffer.append("Walking workspace...\r\n");
        buffer.append("[" + key.substring(0, 6) + "] (WORKSPACE name:" + workspace.getName() + ", clock:" + workspace.getClock() + ")\r\n");
        buffer.append("# HEAD #\r\n");
        walkCollection(buffer, workspace.getHead(), 0);
        for (SnapshotElement snapshot : workspace.getSnapshots()) {
            buffer.append("# SNAPSHOT #\r\n");
            walkCollection(buffer, snapshot.getKey(), 0);
        }
        buffer.append("----------------- Walk done -------------\r\n");
        return buffer.toString();
    }

    private void walkCollection(StringBuffer buffer, String key, int level) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        Collection collection = core.readCollection(key);
        for (int i = 0; i < level; i++) {
            buffer.append(" ");
        }
        buffer.append("[" + key.substring(0, 6) + "] COLLECTION {name:" + collection.getName() + ", clock:" + collection.getClock() + "}\r\n");
        for (MetadataElement element : collection.getMetadatas()) {
            walkMetaDataObject(buffer, element.getName(), element.getKey(), level);
        }
        for (CollectionElement element : collection.getElements()) {
            if (element.getType().equals(Collection.OBJECT_TYPE)) {
                walkCollection(buffer, element.getKey(), level + 1);
            }
            if (element.getType().equals(DataObject.OBJECT_TYPE)) {
                walkDataObject(buffer, element.getKey(), level + 1);
            }
            if (element.getType().equals(Link.OBJECT_TYPE)) {
                walkLink(buffer, element.getKey(), level + 1);
            }
        }
    }

    private void walkDataObject(StringBuffer buffer, String key, int level) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        DataObject object = core.readDataObject(key);
        for (int i = 0; i < level; i++) {
            buffer.append(" ");
        }
        buffer.append("[" + key.substring(0, 6) + "] OBJECT {name:" + object.getName() + ", clock:" + object.getClock() + "}\r\n");
        for (MetadataElement element : object.getMetadatas()) {
            walkMetaDataObject(buffer, element.getName(), element.getKey(), level);
        }
    }

    private void walkLink(StringBuffer buffer, String key, int level) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        Link link = core.readLink(key);
        for (int i = 0; i < level; i++) {
            buffer.append(" ");
        }
        buffer.append("[" + key.substring(0, 6) + "] LINK {name:" + link.getName() + ", clock:" + link.getClock() + "}\r\n");
        for (MetadataElement element : link.getMetadatas()) {
            walkMetaDataObject(buffer, element.getName(), element.getKey(), level);
        }
    }

    private void walkMetaDataObject(StringBuffer buffer, String name, String key, int level) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        for (int i = 0; i < level; i++) {
            buffer.append(" ");
        }
        buffer.append(">>------  METADATA {name: " + name + ", key: " + key.substring(0, 6) + "}\r\n");
    }

}
