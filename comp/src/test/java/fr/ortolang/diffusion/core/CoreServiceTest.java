package fr.ortolang.diffusion.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@RunWith(Arquillian.class)
public class CoreServiceTest {

	private static Logger logger = Logger.getLogger(CoreServiceTest.class.getName());

	@EJB
	private MembershipService membership;
	@EJB
	private CoreService core;

	@Deployment
	public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "diffusion-server-ejb.jar");
		jar.addPackage("fr.ortolang.diffusion");
		// jar.addPackage("fr.ortolang.diffusion.bootstrap");
		jar.addPackage("fr.ortolang.diffusion.browser");
		jar.addPackage("fr.ortolang.diffusion.core");
		jar.addPackage("fr.ortolang.diffusion.core.entity");
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
		jar.addAsResource("config.properties");
		jar.addAsResource("ontology/foaf.xml");
		jar.addAsResource("ontology/ortolang.xml");
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
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
	}

	@After
	public void tearDown() {
		logger.log(Level.INFO, "clearing environment");
	}

	@Test
	public void testLogin() throws LoginException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			String key = membership.getProfileKeyForConnectedIdentifier();
			assertEquals("guest", key);
		} finally {
			loginContext.logout();
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateWorkspaceAsUnauthentifiedUser() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("guest", "password");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			core.createWorkspace("K1", "Blabla", "test");
			fail("Should have raised an AccessDeniedException");
		} finally {
			loginContext.logout();
		}
	}

	@Test(expected = KeyAlreadyExistsException.class)
	public void testCreateWorkspaceWithExistingKey() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
			}
			core.createWorkspace("K2", "Blabla2", "test");
			core.createWorkspace("K2", "Blabla3", "test");
			fail("Should have raised a KeyAlreadyExistsException");
		} finally {
			loginContext.logout();
		}
	}

	@Test(expected = KeyNotFoundException.class)
	public void testReadUnexistingWorkspace() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException,
			KeyNotFoundException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
			}
			core.readWorkspace("UNEXISTING");
			fail("Should have raised a KeyNotFoundException");
		} finally {
			loginContext.logout();
		}
	}

	@Test(expected = AccessDeniedException.class)
	public void testReadUnreadableWorkspace() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException,
			KeyNotFoundException {
		final String WORKSPACE_KEY = "WSP";
		final String WORKSPACE_NAME = "WorkspaceProtected";
		final String WORKSPACE_TYPE = "test";

		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
			}
			core.createWorkspace(WORKSPACE_KEY, WORKSPACE_NAME, WORKSPACE_TYPE);
		} finally {
			loginContext.logout();
		}

		loginContext = AuthenticationLoginContextFactory.createLoginContext("user2", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User TWO", "user.two@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user2 already exists !!");
			}
			core.readWorkspace(WORKSPACE_KEY);
			fail("Should have raised a  AccessDeniedException");
		} finally {
			loginContext.logout();
		}

	}

	@Test
	public void testCRUDWorkspace() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, KeyNotFoundException {
		final String WORKSPACE_KEY = "WS1";
		final String WORKSPACE_NAME = "Workspace1";
		final String WORKSPACE_NAME_UPDATE = "Workspace1.update";
		final String WORKSPACE_TYPE = "test";

		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
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
	public void testCRUDCollection() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException,
			KeyNotFoundException, InvalidPathException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
			}

			String wsk = UUID.randomUUID().toString();
			core.createWorkspace(wsk, "WorkspaceCollection", "test");

			core.createCollection(wsk, "/a", "Collection A");
			core.createCollection(wsk, "/a/b", "Collection B inside A");
			core.createCollection(wsk, "/a/c", "Collection C inside A");

			logger.log(Level.INFO, "Workspace created with 3 collections");
			logger.log(Level.INFO, walkWorkspace(wsk));

			String akey = core.resolveWorkspacePath(wsk, "head", "/a");
			Collection collection = core.readCollection(akey);
			assertEquals(1, collection.getClock());
			assertEquals("a", collection.getName());
			assertEquals("Collection A", collection.getDescription());
			assertEquals(1, collection.getClock());
			assertEquals(2, collection.getElements().size());
			String bkey = core.resolveWorkspacePath(wsk, "head", "/a/b");
			String ckey = core.resolveWorkspacePath(wsk, "head", "/a/c");
			assertTrue(collection.findElementByName("b").getKey().equals(bkey));
			assertTrue(collection.findElementByName("c").getKey().equals(ckey));

			core.updateCollection(wsk, "/a", "COLLECTION A");
			collection = core.readCollection(akey);
			assertEquals(1, collection.getClock());
			assertEquals("a", collection.getName());
			assertEquals("COLLECTION A", collection.getDescription());
			assertEquals(2, collection.getElements().size());
			assertTrue(collection.findElementByName("b").getKey().equals(bkey));
			assertTrue(collection.findElementByName("c").getKey().equals(ckey));

			core.updateCollection(wsk, "/a/c", "Collection C INSIDE A");
			collection = core.readCollection(akey);
			assertTrue(collection.findElementByName("c").getKey().equals(ckey));
			collection = core.readCollection(ckey);
			assertEquals("c", collection.getName());
			assertEquals("Collection C INSIDE A", collection.getDescription());

			core.deleteCollection(wsk, "/a/b");
			collection = core.readCollection(akey);
			assertEquals(1, collection.getClock());
			assertEquals(1, collection.getElements().size());
			assertTrue(collection.findElementByName("b") == null);
			assertTrue(collection.findElementByName("c").getKey().equals(ckey));

			logger.log(Level.INFO, walkWorkspace(wsk));

			Workspace workspace = core.readWorkspace(wsk);
			assertEquals(0, workspace.getSnapshots().size());
			assertEquals(1, workspace.getClock());
			assertTrue(workspace.hasChanged());
			core.snapshotWorkspace(wsk, "v1");
			workspace = core.readWorkspace(wsk);
			assertEquals(1, workspace.getSnapshots().size());
			assertEquals(2, workspace.getClock());
			assertFalse(workspace.hasChanged());

			logger.log(Level.INFO, "Workspace snapshotted");
			logger.log(Level.INFO, walkWorkspace(wsk));

			try {
				core.snapshotWorkspace(wsk, "v2");
				fail("A second snapshot without changes should produce an exception");
			} catch (Exception e) {
				logger.log(Level.INFO, e.getMessage());
			}

			core.updateCollection(wsk, "/a/c", "Collection C INSIDE A, updated");
			core.createCollection(wsk, "/a/d", "New Collection D INSIDE A");
			logger.log(Level.INFO, walkWorkspace(wsk));

			core.snapshotWorkspace(wsk, "v2");
			workspace = core.readWorkspace(wsk);
			assertEquals(2, workspace.getSnapshots().size());
			assertEquals(3, workspace.getClock());
			assertFalse(workspace.hasChanged());

			core.deleteCollection(wsk, "/a/d");
			logger.log(Level.INFO, walkWorkspace(wsk));

			core.createCollection(wsk, "/a/e", "Collection E");
			core.snapshotWorkspace(wsk, "v3");
			logger.log(Level.INFO, walkWorkspace(wsk));

			core.createCollection(wsk, "/a/c/f", "Third level Collection F");
			logger.log(Level.INFO, walkWorkspace(wsk));

			core.moveCollection(wsk, "/a/c/f", "/a/e/f");
			logger.log(Level.INFO, walkWorkspace(wsk));

			core.snapshotWorkspace(wsk, "v4");
			core.moveCollection(wsk, "/a/e/f", "/a/c/g");
			logger.log(Level.INFO, walkWorkspace(wsk));

		} finally {
			loginContext.logout();
		}
	}

	@Test
	public void testDeleteCollectionElementConcurrently() throws LoginException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException,
			MembershipServiceException, KeyNotFoundException, InvalidPathException {
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
		loginContext.login();
		try {
			logger.log(Level.INFO, membership.getProfileKeyForConnectedIdentifier());
			try {
				membership.createProfile("User ONE", "user.one@ortolang.fr");
			} catch (ProfileAlreadyExistsException e) {
				logger.log(Level.INFO, "Profile user1 already exists !!");
			}

			String wsk = UUID.randomUUID().toString();
			core.createWorkspace(wsk, "WorkspaceCollection", "test");

			core.createCollection(wsk, "/a", "Collection A");
			core.createCollection(wsk, "/a/a", "Collection A inside A");
			core.createCollection(wsk, "/a/b", "Collection B inside A");
			core.createCollection(wsk, "/a/c", "Collection C inside A");
			core.createCollection(wsk, "/a/d", "Collection D inside A");
			core.createCollection(wsk, "/a/e", "Collection E inside A");
			core.createCollection(wsk, "/a/f", "Collection F inside A");
			core.createCollection(wsk, "/a/g", "Collection G inside A");
			core.createCollection(wsk, "/a/h", "Collection H inside A");
			core.createCollection(wsk, "/a/i", "Collection I inside A");
			core.createCollection(wsk, "/a/j", "Collection J inside A");

			logger.log(Level.INFO, "Workspace created");
			logger.log(Level.INFO, walkWorkspace(wsk));
			
			String key = core.resolveWorkspacePath(wsk, "head", "/a");
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
				logger.log(Level.INFO, "wait for threads to finish");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					logger.log(Level.INFO, "interrupted", e);
				}
			}
			
			int expectedSize = initialSize;
			for (CollectionElementDeleter deleter : deleters) {
				if ( deleter.hasBeenDeleted() ) {
					expectedSize--;
				}
			}

			key = core.resolveWorkspacePath(wsk, "head", "/a");
			col = core.readCollection(key);

			assertEquals(expectedSize, col.getElements().size());

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
				LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("user1", "tagada");
				loginContext.login();
				try {
					logger.log(Level.INFO, "Deleting collection at path: " + path);
					core.deleteCollection(wskey, path);
					deleted = true;
				} catch (CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException e) {
					logger.log(Level.SEVERE, "error during deleting collection: " + e.getMessage());
				} finally {
					loginContext.logout();
				}
			} catch (LoginException e) {
				logger.log(Level.SEVERE, "unable to login", e);
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
