package fr.ortolang.diffusion.store.handle;

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginException;
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

import fr.ortolang.diffusion.store.handle.entity.Handle;

@RunWith(Arquillian.class)
public class HandleStoreServiceTest {
	
	private static final Logger LOGGER = Logger.getLogger(HandleStoreServiceTest.class.getName());
	
	@PersistenceContext
    private EntityManager em;
    
    @Resource(name="java:jboss/UserTransaction")
    private UserTransaction utx;
    
    @EJB
    private HandleStoreService service;
	
	@Deployment
    public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "handle-store.jar");
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.store.handle");
		jar.addPackage("fr.ortolang.diffusion.store.handle.entity");
		jar.addClass("fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory");
		jar.addAsResource("config.properties");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
        LOGGER.log(Level.INFO, "Created JAR for test : " + jar.toString(true));
        
        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-handle-store.ear");
		ear.addAsModule(jar);
		ear.addAsLibraries(pom.resolve("net.handle:hdlnet-hclj:7.3.1").withTransitivity().asFile());
		LOGGER.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
    }
 
	@Before
	public void setup() throws Exception {
		LOGGER.log(Level.INFO, "Setting up test environment, clearing data");
		utx.begin();
	    em.joinTransaction();
		em.createQuery("delete from Handle").executeUpdate();
	    utx.commit();
	}
	
	@After
	public void tearDown() {
		LOGGER.log(Level.INFO, "clearing environment");
	}

	@Test
	public void testRecord() throws LoginException {
		try {
			service.recordHandle("11403/666", "mykey", "http://www.free.fr");
			List<Handle> values = service.listHandleValues("11403/666");
			assertEquals(1, values.size());
			for ( Handle handle : values ) {
				LOGGER.log(Level.INFO, "VALUE " + handle);
			}
		} catch (HandleStoreServiceException | HandleNotFoundException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFind() throws LoginException {
		try {
			service.recordHandle("11403/111", "K1", "http://www.free.fr");
			service.recordHandle("11403/222", "K1", "http://www.google.com");
			service.recordHandle("11403/333", "K2", "http://www.atilf.fr");
			service.recordHandle("11403/444", "K2", "http://www.cnrs.fr");
			service.recordHandle("11403/555", "K3", "http://www.facebook.com");
			List<String> namesK1 = service.listHandlesForKey("K1");
			List<String> namesK2 = service.listHandlesForKey("K2");
			List<String> namesK3 = service.listHandlesForKey("K3");
			assertEquals(2, namesK1.size());
			assertTrue(namesK1.contains("11403/111"));
			assertTrue(namesK1.contains("11403/222"));
			assertEquals(2, namesK2.size());
			assertEquals(1, namesK3.size());
			
			service.recordHandle("11403/444", "K3", "http://www.cnrs.fr");
			namesK1 = service.listHandlesForKey("K1");
			namesK2 = service.listHandlesForKey("K2");
			namesK3 = service.listHandlesForKey("K3");
			assertEquals(2, namesK1.size());
			assertEquals(1, namesK2.size());
			assertFalse(namesK2.contains("11403/444"));
			assertEquals(2, namesK3.size());
		} catch (HandleStoreServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testDeleteHandle() throws LoginException {
		try {
			service.recordHandle("11403/111", "K1", "http://www.free.fr");
			service.recordHandle("11403/222", "K1", "http://www.google.com");
			service.recordHandle("11403/333", "K2", "http://www.atilf.fr");
			service.recordHandle("11403/444", "K2", "http://www.cnrs.fr");
			service.recordHandle("11403/555", "K3", "http://www.facebook.com");
			List<String> namesK1 = service.listHandlesForKey("K1");
			assertEquals(2, namesK1.size());
			assertTrue(namesK1.contains("11403/111"));
			assertTrue(namesK1.contains("11403/222"));
			
			service.dropHandle("11403/222");
			namesK1 = service.listHandlesForKey("K1");
			assertEquals(1, namesK1.size());
			assertFalse(namesK1.contains("11403/222"));
		} catch (HandleStoreServiceException e) {
			fail(e.getMessage());
		}
	}
	
}
