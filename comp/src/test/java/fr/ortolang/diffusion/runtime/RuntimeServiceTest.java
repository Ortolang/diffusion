package fr.ortolang.diffusion.runtime;

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
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

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
		jar.addAsResource("test-activiti.cfg.xml", "activiti.cfg.xml");
		jar.addAsResource("runtime/HelloWorld.bpmn");
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
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			String key = membership.getProfileKeyForConnectedIdentifier();
			assertEquals("guest", key);
		} finally {
			loginContext.logout();
		}
	}

	@Test
	public void testListProcessTypes() throws LoginException {
		LoginContext loginContext = UsernamePasswordLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			List<ProcessType> types = runtime.listProcessTypes();
			assertTrue(types.size() > 0);
		} catch (RuntimeServiceException e) {
			fail(e.getMessage());
			logger.log(Level.WARNING, e.getMessage(), e);
		} finally {
			loginContext.logout();
		}
	}
	
//	@Test
//	public void testCreateProcess() throws LoginException, KeyAlreadyExistsException, AccessDeniedException, RuntimeServiceException, IOException, KeyNotFoundException {
//		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
//		loginContext.login();
//		try {
//			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
//			runtime.createProcess("K2", this.getClass().getClassLoader().getResourceAsStream("workflows/HelloWorldProcess.bpmn"));
//			
//			Map<String, Object> params = new HashMap<String, Object>();
//			params.put("name", "junit");
//			runtime.startProcessInstance("K3", "K2", params);
//			
//			Process ins = runtime.readProcessInstance("K3");
//			
//			assertEquals("K3", ins.getKey());
//			assertEquals("root", ins.getInitier());
//		} finally {
//			loginContext.logout();
//		}
//	}
	
}