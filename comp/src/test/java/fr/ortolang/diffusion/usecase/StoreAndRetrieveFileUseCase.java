package fr.ortolang.diffusion.usecase;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

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
		jar.addPackage("fr.ortolang.diffusion.registry.entity");
		jar.addPackage("fr.ortolang.diffusion.store");
		jar.addPackage("fr.ortolang.diffusion.store.binary");
		jar.addPackage("fr.ortolang.diffusion.store.binary.hash");
		jar.addPackage("fr.ortolang.diffusion.usecase");
		jar.addPackage("fr.ortolang.diffusion.notification");
		jar.addAsResource("config.properties");
		jar.addAsResource("file1.jpg");
		logger.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
		ear.addAsModule(jar);
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("javax.activation:activation:1.1.1").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("commons-io:commons-io:2.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("com.healthmarketscience.rmiio:rmiio:2.0.4").withTransitivity()
				.asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.wildfly:wildfly-ejb-client-bom:pom:8.0.0.Beta1")
				.withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.tika:tika-core:1.4")
				.withTransitivity().asFile());
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
	}

	@Test
	public void testHostSimpleFile() throws URISyntaxException {
		// Path origin = Paths.get(HostAndRetrieveFileTest.class.getClassLoader().getResource("file1.jpg").getPath());
		Path origin = Paths.get("/home/jerome/Images/test.jpg");
		logger.log(Level.INFO, "Origin file to insert in container : " + origin.toString());

		Path destination = Paths.get("/tmp/" + System.currentTimeMillis());
		logger.log(Level.INFO, "Destination file for retrieving content from container : " + destination.toString());

		String wkey = UUID.randomUUID().toString();
		String okey = UUID.randomUUID().toString();
		
		// Create a Workspace
		try {
			core.createWorkspace(wkey, "Test Workspace", "test");
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Create the Digital Object
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Files.copy(origin, baos);
			//core.createDataObject(wkey, "/" + okey, "Test Object", "A really simple test object !!", baos.toByteArray());
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Check that the object is registered in the browser
		try {
			OrtolangObjectIdentifier identifier = browser.lookup(okey);
			assertEquals(identifier.getService(), CoreService.SERVICE_NAME);
			assertEquals(identifier.getType(), DataObject.OBJECT_TYPE);
		} catch (BrowserServiceException | KeyNotFoundException | AccessDeniedException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Retrieve this digital object informations using the key
		try {
			DataObject object = core.readDataObject(okey);
			logger.log(Level.INFO, "Detected mime type : " + object.getMimeType());
			logger.log(Level.INFO, "Detected size : " + object.getSize());
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}

		// Retrieve this digital object data using the key
		try {
			//byte[] data = core.readDataObjectContent(okey);
			//Files.copy(new ByteArrayInputStream(data), destination);
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

	}

}
