package fr.ortolang.diffusion.message;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.io.ByteArrayInputStream;
import java.util.List;
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

import fr.ortolang.diffusion.core.AliasAlreadyExistsException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@RunWith(Arquillian.class)
public class MessageServiceTest {

    private static final Logger LOGGER = Logger.getLogger(MessageServiceTest.class.getName());

    @EJB
    private MessageService message;
    @EJB
    private CoreService core;
    @EJB
    private MembershipService membership;
    @EJB
    private RegistryService registry;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "diffusion-server-ejb.jar");
        jar.addPackage("fr.ortolang.diffusion");
        jar.addPackage("fr.ortolang.diffusion.browser");
        jar.addPackage("fr.ortolang.diffusion.core");
        jar.addPackage("fr.ortolang.diffusion.core.entity");
        jar.addPackage("fr.ortolang.diffusion.event");
        jar.addPackage("fr.ortolang.diffusion.event.entity");
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
        jar.addClass("fr.ortolang.diffusion.store.json.JsonStoreServiceException");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexablePlainTextContent");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexStoreDocumentBuilder");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexStoreService");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexStoreServiceBean");
        jar.addClass("fr.ortolang.diffusion.store.index.IndexStoreServiceException");
        jar.addAsResource("config.properties");
        jar.addAsResource("schema/ortolang-item-schema.json");
        jar.addAsResource("schema/ortolang-workspace-schema.json");
        jar.addAsResource("json/meta.json");
        jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "diffusion-server-ear.ear");
        ear.addAsModule(jar);
        ear.addAsLibraries(pom.resolve("commons-io:commons-io:2.4").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.wildfly:wildfly-ejb-client-bom:pom:9.0.1.Final").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.tika:tika-core:1.7").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-core:4.6.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-highlighter:4.6.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-analyzers-common:4.6.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.lucene:lucene-queryparser:4.6.0").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.tika:tika-core:1.8").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.apache.tika:tika-parsers:1.8").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("com.github.fge:json-schema-validator:2.2.6").withTransitivity().asFile());
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
    public void testCreateThreadAsUnauthentifiedUser() throws LoginException, MessageServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException {
        LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
        try {
            membership.createProfile("Anonymous", "", "anonymous@ortolang.fr");
        } catch (ProfileAlreadyExistsException e) {
            LOGGER.log(Level.INFO, "Profile anonymous already exists !!");
        }
        message.createThread("K1", "WSK1", "TestThread", "Blabla", false);
        fail("Should have raised an AccessDeniedException");
    }

    @Test
    public void testThread() throws LoginException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, MessageServiceException, CoreServiceException, AliasAlreadyExistsException, KeyNotFoundException, DataCollisionException {
        LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
        loginContext.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            try {
                membership.createProfile("User", "ONE", "user.one@ortolang.fr");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "Profile user1 already exists !!");
            }
            
            core.createWorkspace("WSK1", "wsone", "WorkspaceOne", WorkspaceType.USER.toString());
            
            message.createThread("K1", "WSK1", "TestFeed", "A Test MEssage Feed", false);
            Thread mf = message.readThread("K1");
            assertEquals("TestFeed", mf.getName());
            assertEquals("A Test MEssage Feed", mf.getDescription());
            assertEquals("K1", mf.getKey());
            assertEquals("WSK1", mf.getWorkspace());
            
            message.updateThread("K1", "TestFeedo", "A Test MEssage FeedOO !!");
            mf = message.readThread("K1");
            assertEquals("TestFeedo", mf.getName());
            assertEquals("A Test MEssage FeedOO !!", mf.getDescription());
            assertEquals("K1", mf.getKey());
            assertEquals("WSK1", mf.getWorkspace());
            
            List<String> mfs = message.findThreadsForWorkspace("WSK1");
            assertTrue(mfs.contains("K1"));
            
            List<Message> messages = message.browseThread("K1", 0, 25);
            assertEquals(0, messages.size());
            
            message.postMessage("K1", "MK1", "", "Does anybody can help me ??", "This is only a test message in order to ask for a question based on a logical answer that will produce some text using anything else than simple words...");
            message.postMessage("K1", "MK2", "MK1", "Why do you say that !", "Stop fillign this feed with some stupid content that does not interest anybody !");
            message.postMessage("K1", "MK3", "", "Somebody over there ??", "dadaduc");
            
            Message msg = message.readMessage("MK2");
            assertEquals("MK2", msg.getKey());
            assertEquals("Why do you say that !", msg.getTitle());
            assertTrue(msg.getBody().contains("fillign"));
            
            message.updateMessage("MK2", "Why do you say that ?", "Stop filling this feed with some stupid content that does not interest anybody !");
            msg = message.readMessage("MK2");
            assertEquals("MK2", msg.getKey());
            assertEquals("Why do you say that ?", msg.getTitle());
            assertTrue(msg.getBody().contains("filling"));
            
            messages = message.browseThread("K1", 0, 25);
            assertEquals(3, messages.size());
            
            message.postMessage("K1", "MK4", "MK3", "Try attachments", "et hop !");
            message.addMessageAttachment("MK4", "test.txt", new ByteArrayInputStream("This is a simple text content".getBytes()));
            message.addMessageAttachment("MK4", "test.xml", new ByteArrayInputStream("<body>This is another simple text content</body>".getBytes()));
            
            msg = message.readMessage("MK4");
            assertEquals(2, msg.getAttachments().size());
            
            message.removeMessageAttachment("MK4", "test.xml");
            msg = message.readMessage("MK4");
            assertEquals(1, msg.getAttachments().size());
            
            messages = message.browseThread("K1", 0, 25);
            assertEquals(4, messages.size());
            
            message.deleteMessage("MK4");
            
            messages = message.browseThread("K1", 0, 25);
            assertEquals(3, messages.size());
            
            message.deleteThread("K1");
            
        } finally {
            loginContext.logout();
        }
    }
}