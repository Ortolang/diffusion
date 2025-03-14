package fr.ortolang.diffusion.membership;

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
import static org.junit.Assert.fail;

import java.util.List;
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
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;

@RunWith(Arquillian.class)
public class MembershipServiceTest {

    private static final Logger LOGGER = Logger.getLogger(MembershipServiceTest.class.getName());

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
        jar.addPackage("fr.ortolang.diffusion.browser");
        jar.addPackage("fr.ortolang.diffusion.core");
        jar.addPackage("fr.ortolang.diffusion.core.entity");
        jar.addPackage("fr.ortolang.diffusion.core.wrapper");
        jar.addPackage("fr.ortolang.diffusion.event");
        jar.addPackage("fr.ortolang.diffusion.event.entity");
        jar.addPackage("fr.ortolang.diffusion.extraction");
        jar.addPackage("fr.ortolang.diffusion.indexing");
        jar.addPackage("fr.ortolang.diffusion.jobs");
        jar.addPackage("fr.ortolang.diffusion.jobs.entity");
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
        jar.addClass("fr.ortolang.diffusion.membership.indexing.ProfileIndexableContent");
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
        jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "diffusion-server-ear.ear");
        ear.addAsModule(jar);
        ear.addAsLibraries(pom.resolve("org.wildfly:wildfly-ejb-client-bom:pom:9.0.1.Final").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("org.codehaus.jettison:jettison:1.3.3").withTransitivity().asFile());
        ear.addAsLibraries(pom.resolve("com.healthmarketscience.rmiio:rmiio:2.0.4").withTransitivity().asFile());
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
    public void setup() throws MembershipServiceException, AccessDeniedException {
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

    @Test
    public void testProfileInfos() throws LoginException, MembershipServiceException, AccessDeniedException {
        LoginContext loginContextRoot = UsernamePasswordLoginContextFactory.createLoginContext("root", "tagada54");
        loginContextRoot.login();
        try {
            LOGGER.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
            LOGGER.log(Level.FINE, "creating root profile");
            membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super", "User", "root@ortolang.org", ProfileStatus.ACTIVE);
            LOGGER.log(Level.FINE, "creating anonymous profile");
            membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Anonymous", "User", "anonymous@ortolang.org", ProfileStatus.ACTIVE);

            membership.createProfile("user1", "User", "One", "user1@ortolang.fr", ProfileStatus.ACTIVE);
            membership.createProfile("user2", "User", "Two", "user2@ortolang.fr", ProfileStatus.ACTIVE);

            LOGGER.log(Level.FINE, "adding user2 as friend");
            String friendGroupKey = membership.readProfile("root").getFriends();
            membership.addMemberInGroup(friendGroupKey, "user2");

            String key = membership.getProfileKeyForConnectedIdentifier();
            assertEquals("root", key);
        } catch ( ProfileAlreadyExistsException e ) {
            LOGGER.log(Level.INFO, "Profile already exists");
        } catch ( KeyNotFoundException e) {
            fail(e.getMessage());
        }
        try {
            // Create infos
            membership.setProfileInfo("root", "presentation.prop1", "value1", ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
            membership.setProfileInfo("root", "presentation.prop2", "value2", ProfileDataVisibility.FRIENDS, ProfileDataType.STRING, "");
            membership.setProfileInfo("root", "presentation.prop3", "value3", ProfileDataVisibility.NOBODY, ProfileDataType.STRING, "");
            membership.setProfileInfo("root", "setting.prop4", "value4", ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
            membership.setProfileInfo("root", "setting.prop5", "value5", ProfileDataVisibility.NOBODY, ProfileDataType.STRING, "");

            // Get infos with root profile
            LOGGER.log(Level.INFO, "TEST1 : root should see all infos of his own profile.");
            List<ProfileData> infosSeenByRoot = membership.listProfileInfos("root", "");
            assertEquals(5, infosSeenByRoot.size());

            // Consult infos of category "presentation"
            LOGGER.log(Level.INFO, "TEST2 : root should only see root's infos of category 'presentation'.");
            List<ProfileData> presentationSeenByRoot = membership.listProfileInfos("root", "presentation");
            assertEquals(3, presentationSeenByRoot.size());

            loginContextRoot.logout();

            LoginContext loginContextUser1 = UsernamePasswordLoginContextFactory.createLoginContext("user1", "tagada");
            loginContextUser1.login();

            // Get infos with jmarple profile
            LOGGER.log(Level.INFO, "TEST3 : user1 should only see root's infos with visibility set to EVERYBODY.");
            List<ProfileData> infosSeenByUser1 = membership.listProfileInfos("root", "");
            assertEquals(2, infosSeenByUser1.size());

            // Consult infos of category "presentation"
            LOGGER.log(Level.INFO, "TEST4 : user1 should only see root's infos of category 'presentation' and with visibility set to EVERYBODY.");
            List<ProfileData> presentationSeenByUser1 = membership.listProfileInfos("root", "presentation");
            assertEquals(1, presentationSeenByUser1.size());

            loginContextUser1.logout();

            LoginContext loginContextUser2 = UsernamePasswordLoginContextFactory.createLoginContext("user2", "tagada");
            loginContextUser2.login();

            // Get infos with sholmes profile (friend of root)
            LOGGER.log(Level.INFO, "TEST5 : user2 should only see root's infos with visibility set to EVERYBODY OR FRIENDS.");
            List<ProfileData> infosSeenByUser2 = membership.listProfileInfos("root", "");
            assertEquals(3, infosSeenByUser2.size());

            // Consult infos of category "presentation"
            LOGGER.log(Level.INFO, "TEST6 : user2 should only see root's infos of category 'presentation' and with visibility set to EVERYBODY or FRIENDS.");
            List<ProfileData> presentationSeenByUser2 = membership.listProfileInfos("root", "presentation");
            assertEquals(2, presentationSeenByUser2.size());

            loginContextUser2.logout();

            LoginContext loginContextUser3 = UsernamePasswordLoginContextFactory.createLoginContext("user3", "tagada");
            loginContextUser3.login();

            LOGGER.log(Level.INFO, "TEST7 : user3 should be able to create his own profile.");
            membership.createProfile("user3", "User", "Three", "user3@ortolang.fr", ProfileStatus.ACTIVE);

            LOGGER.log(Level.INFO, "TEST8 : user3 should not be able to create user4 profile.");
            try {
                membership.createProfile("user4", "User", "Four", "user4@ortolang.fr", ProfileStatus.ACTIVE);
                fail("Should have raise an AccessDeniedException");
            } catch (AccessDeniedException e) {
                LOGGER.log(Level.INFO, "AccessDeniedException");
            }

            LOGGER.log(Level.INFO, "TEST9 : user3 should not be able to create user3 profile again.");
            try {
                membership.createProfile("user3", "User", "Three", "user3@ortolang.fr", ProfileStatus.ACTIVE);
                fail("Should have raise a ProfileAlreadyExistsException");
            } catch (ProfileAlreadyExistsException e) {
                LOGGER.log(Level.INFO, "ProfileAlreadyExistsException");
            }

            loginContextUser3.logout();

        } catch ( MembershipServiceException | KeyNotFoundException | ProfileAlreadyExistsException e) {
            fail(e.getMessage());
        }
    }

}
