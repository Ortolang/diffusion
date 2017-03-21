package fr.ortolang.diffusion.indexing;

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

import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.registry.RegistryService;
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

import javax.ejb.EJB;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class IndexingServiceTest {

    private static final Logger LOGGER = Logger.getLogger(IndexingServiceTest.class.getName());

    @EJB
    private RegistryService registry;
    @EJB
    private ReferentialService referential;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "components.jar");
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
        jar.addPackage("fr.ortolang.diffusion.referential");
        jar.addPackage("fr.ortolang.diffusion.referential.entity");
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
        jar.addPackage("fr.ortolang.diffusion.store.index");
        jar.addPackage("fr.ortolang.diffusion.jobs");
        jar.addPackage("fr.ortolang.diffusion.jobs.entity");
        jar.addPackage("fr.ortolang.diffusion.template");
        jar.addAsResource("config.properties");
        jar.addAsResource("orientdb-config.xml");
        jar.addAsResource("schema/ortolang-item-schema.json");
        jar.addAsResource("schema/ortolang-workspace-schema.json");
        jar.addAsResource("json/sample3.json");
        jar.addAsResource("json/sample4.json");
        jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "diffusion.ear");
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
    public void testOrtolangIndexableContentParser() throws IOException {
        String json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("json/sample3.json"), Charset.defaultCharset());
        try {
            String parse = OrtolangIndexableContentParser.parse(json);
            assertEquals(json, parse);
        } catch (IndexableContentParsingException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            fail("Should not have raised an IndexableContentParsingException");
        }

        json = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("json/sample4.json"), Charset.defaultCharset());
        // TODO
//        String expect = json.replace("${foobar}")
    }
}
