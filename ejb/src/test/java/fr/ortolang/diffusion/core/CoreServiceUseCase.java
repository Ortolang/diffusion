package fr.ortolang.diffusion.core;

import static fr.ortolang.diffusion.SaveParamsAction.saveParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

@RunWith(Arquillian.class)
public class CoreServiceUseCase {

	private static Logger logger = Logger.getLogger(CoreServiceUseCase.class.getName());

	private Mockery mockery;
	private RegistryService registry;
	private BinaryStoreService binary;
	private MembershipService membership;
	private NotificationService notification;
	private IndexingService indexing;
	private CoreService core;

	@PersistenceContext
	private EntityManager em;

	@Resource(name = "java:jboss/UserTransaction")
	private UserTransaction utx;
	
	@Deployment
	public static EnterpriseArchive createDeployment() {
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "core.jar");
		jar.addPackage("fr.ortolang.diffusion");
		jar.addPackage("fr.ortolang.diffusion.core");
		jar.addPackage("fr.ortolang.diffusion.core.entity");
		jar.addPackage("fr.ortolang.diffusion.test.core");
		jar.addPackage("fr.ortolang.diffusion.membership");
		jar.addPackage("fr.ortolang.diffusion.membership.entity");
		jar.addPackage("fr.ortolang.diffusion.security.authentication");
		jar.addPackage("fr.ortolang.diffusion.registry");
		jar.addPackage("fr.ortolang.diffusion.registry.entity");
		jar.addPackage("fr.ortolang.diffusion.store.binary");
		jar.addPackage("fr.ortolang.diffusion.store.binary.hash");
		jar.addPackage("fr.ortolang.diffusion.store.index");
		jar.addPackage("fr.ortolang.diffusion.notification");
		jar.addPackage("fr.ortolang.diffusion.indexing");
		jar.addAsResource("config.properties");
		jar.addAsManifestResource("test-persistence.xml", "persistence.xml");
		logger.log(Level.INFO, "Created JAR for test : " + jar.toString(true));

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-core.ear");
		ear.addAsModule(jar);
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.jmock:jmock-junit4:2.5.1").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("commons-io:commons-io:2.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("com.healthmarketscience.rmiio:rmiio:2.0.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.wildfly:wildfly-ejb-client-bom:pom:8.0.0.Beta1").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.tika:tika-core:1.4").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.lucene:lucene-core:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.lucene:lucene-highlighter:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.lucene:lucene-analyzers-common:4.6.0").withTransitivity().asFile());
		ear.addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.lucene:lucene-queryparser:4.6.0").withTransitivity().asFile());
		logger.log(Level.INFO, "Created EAR for test : " + ear.toString(true));

		return ear;
	}

	@Before
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
		try {
			mockery = new Mockery();
			registry = mockery.mock(RegistryService.class);
			binary = mockery.mock(BinaryStoreService.class);
			membership = mockery.mock(MembershipService.class);
			notification = mockery.mock(NotificationService.class);
			indexing = mockery.mock(IndexingService.class);
			core = new CoreServiceBean();
			((CoreServiceBean) core).setRegistryService(registry);
			((CoreServiceBean) core).setBinaryStoreService(binary);
			((CoreServiceBean) core).setMembershipService(membership);
			((CoreServiceBean) core).setNotificationService(notification);
			((CoreServiceBean) core).setIndexingService(indexing);
			((CoreServiceBean) core).setEntityManager(em);
			utx.begin();
			em.joinTransaction();
			em.createQuery("delete from DigitalObject").executeUpdate();
			em.createQuery("delete from DigitalCollection").executeUpdate();
			em.createQuery("delete from DigitalMetadata").executeUpdate();
			em.createQuery("delete from DigitalReference").executeUpdate();
			utx.commit();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@After
	public void tearDown() {
		logger.log(Level.INFO, "clearing environment");
	}

	@Test
	public void testCRUDObject() {
		final String caller = "profile:guest";
		final Sequence sequence = mockery.sequence("sequence1");
		final Vector<Object> identifier = new Vector<Object>();
		final Vector<Object> identifierClone = new Vector<Object>();

		try {
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(binary).put(with(any(InputStream.class)));
					will(returnValue("sha1-object1"));
					inSequence(sequence);
					oneOf(binary).size(with(equal("sha1-object1")));
					will(returnValue(27l));
					inSequence(sequence);
					oneOf(binary).type(with(equal("sha1-object1")));
					will(returnValue("text/plain"));
					inSequence(sequence);
					oneOf(registry).register(with(equal("K1")), with(any(OrtolangObjectIdentifier.class)));
					will(saveParams(identifier));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.AUTHOR)), with(equal(caller)));
					inSequence(sequence);
					oneOf(indexing).index(with(equal("K1")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.createDataObject("K1", "Name1", "This is the object one", "A little bit of sample data".getBytes());
			utx.commit();
			
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			DataObject object = core.readDataObject("K1");
			utx.commit();
			assertEquals("K1", object.getKey());
			assertEquals("Name1", object.getName());
			assertEquals("This is the object one", object.getDescription());
			assertEquals("text/plain", object.getContentType());
			assertEquals(27, object.getSize());
			assertEquals(0, object.getNbReads());
			assertTrue(object.getStreams().containsValue("sha1-object1"));

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(binary).get(with(equal("sha1-object1")));
					will(returnValue(new ByteArrayInputStream("A little bit of sample data".getBytes())));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read-data"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			byte[] data = core.readDataObjectContent("K1");
			utx.commit();
			assertEquals(27, data.length);

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(indexing).reindex(with(equal("K1")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.updateDataObject("K1", "Name11", "This is the object one updated");
			utx.commit();

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			object = core.readDataObject("K1");
			utx.commit();
			assertEquals("K1", object.getKey());
			assertEquals("Name11", object.getName());
			assertEquals("This is the object one updated", object.getDescription());
			assertEquals(1, object.getNbReads());

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(registry).register(with(equal("K2")), with(any(OrtolangObjectIdentifier.class)), with(equal("K1")), with(equal(false)));
					will(saveParams(identifierClone));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.AUTHOR)), with(equal(caller)));
					inSequence(sequence);
					oneOf(indexing).index(with(equal("K2")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "clone"))), with(any(String.class)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K2")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.cloneDataObject("K2", "K1");
			utx.commit();

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K2")));
					will(returnValue((OrtolangObjectIdentifier) identifierClone.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K2")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			DataObject clone = core.readDataObject("K2");
			utx.commit();
			assertEquals("K2", clone.getKey());
			assertEquals("Name11", clone.getName());
			assertEquals("This is the object one updated", clone.getDescription());
			assertEquals("text/plain", clone.getContentType());
			assertEquals(27, clone.getSize());
			assertEquals(0, clone.getNbReads());
			assertTrue(clone.getStreams().containsValue("sha1-object1"));

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifierClone.elementAt(1)));
					inSequence(sequence);
					oneOf(registry).delete(with(equal("K1")));
					inSequence(sequence);
					oneOf(indexing).remove(with(equal("K1")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "delete"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.deleteDataObject("K1");
			utx.commit();
			
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DataObject.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			try {
				utx.begin();
				em.joinTransaction();
				core.readDataObject("K1");
				utx.commit();
				// TODO Should generate an exception
				// fail("This object is deleted, should give an exception !!");
			} catch (Exception e) {
				// Normal
			}

			mockery.assertIsSatisfied();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testCRUDCollection() {
		final String caller = "profile:guest";
		final Sequence sequence = mockery.sequence("sequence1");
		final Vector<Object> identifier = new Vector<Object>();
		final Vector<Object> identifier2 = new Vector<Object>();
		//final Vector<Object> identifierClone = new Vector<Object>();

		try {
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					
					oneOf(registry).register(with(equal("K1")), with(any(OrtolangObjectIdentifier.class)));
					will(saveParams(identifier));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.AUTHOR)), with(equal(caller)));
					inSequence(sequence);
					
					oneOf(indexing).index(with(equal("K1")));
					inSequence(sequence);
					
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.createCollection("K1", "Name1", "This is the object one");
			utx.commit();
			
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			Collection collection = core.readCollection("K1");
			utx.commit();
			assertEquals("K1", collection.getKey());
			assertEquals("Name1", collection.getName());
			assertEquals("This is the object one", collection.getDescription());

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					//TODO need to update last update timestamp when update a collection ??
//					oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
//					inSequence(sequence);
					oneOf(indexing).reindex(with(equal("K1")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "update"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.updateCollection("K1", "Name11", "This is the object one updated");
			utx.commit();

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			collection = core.readCollection("K1");
			utx.commit();
			assertEquals("K1", collection.getKey());
			assertEquals("Name11", collection.getName());
			assertEquals("This is the object one updated", collection.getDescription());

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					
					oneOf(registry).register(with(equal("K2")), with(any(OrtolangObjectIdentifier.class)));
					will(saveParams(identifier2));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.AUTHOR)), with(equal(caller)));
					inSequence(sequence);
					
					oneOf(indexing).index(with(equal("K2")));
					inSequence(sequence);
					
					oneOf(notification).throwEvent(with(equal("K2")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.createCollection("K2", "Name2", "This is the object two");
			utx.commit();

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);

					oneOf(registry).lookup(with(equal("K2")));
					will(returnValue((OrtolangObjectIdentifier) identifier2.elementAt(1)));
					inSequence(sequence);
					// isMember
					oneOf(registry).lookup(with(equal("K2")));
					will(returnValue((OrtolangObjectIdentifier) identifier2.elementAt(1)));
					inSequence(sequence);

					oneOf(indexing).reindex(with(equal("K1")));
					inSequence(sequence);
					
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "add-element"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.addElementToCollection("K1", "K2", false);
			utx.commit();
			

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			collection = core.readCollection("K1");
			utx.commit();
			assertEquals("K1", collection.getKey());
			assertEquals(1, collection.getElements().size());
			assertTrue(collection.getElements().contains("K2"));


			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);

					oneOf(indexing).reindex(with(equal("K1")));
					inSequence(sequence);
					
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "remove-element"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.removeElementFromCollection("K1", "K2");
			utx.commit();
			
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(Collection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			collection = core.readCollection("K1");
			utx.commit();
			assertEquals("K1", collection.getKey());
			assertEquals(0, collection.getElements().size());
/*
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					
					oneOf(registry).create(with(equal("K3")), with(any(OrtolangObjectIdentifier.class)), with(equal("K1")));
					will(saveParams(identifierClone));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K3")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K3")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K3")), with(equal(OrtolangObjectProperty.AUTHOR)), with(equal(caller)));
					inSequence(sequence);
					oneOf(registry).setProperty(with(equal("K3")), with(equal(OrtolangObjectProperty.OWNER)), with(equal(caller)));
					inSequence(sequence);
					
					oneOf(indexing).index(with(equal("K3")));
					inSequence(sequence);
					
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DigitalCollection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "clone"))), with(any(String.class)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K3")), with(equal(caller)), with(equal(DigitalCollection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "create"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.cloneCollection("K3", "K1");
			utx.commit();

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K3")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K3")), with(equal(caller)), with(equal(DigitalCollection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			DigitalCollection clone = core.readCollection("K3");
			utx.commit();
			assertEquals("K3", clone.getKey());
			assertEquals("Name11", clone.getName());
			assertEquals("This is the object one updated", clone.getDescription());
			assertEquals(0, clone.getElements().size());

			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifierClone.elementAt(1)));
					inSequence(sequence);
					oneOf(registry).delete(with(equal("K1")));
					inSequence(sequence);
					oneOf(indexing).remove(with(equal("K1")));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DigitalCollection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "delete"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			utx.begin();
			em.joinTransaction();
			core.deleteObject("K1");
			utx.commit();
			
			mockery.checking(new Expectations() {
				{
					oneOf(membership).getProfileKeyForConnectedIdentifier();
					will(returnValue(caller));
					inSequence(sequence);
					oneOf(registry).lookup(with(equal("K1")));
					will(returnValue((OrtolangObjectIdentifier) identifier.elementAt(1)));
					inSequence(sequence);
					oneOf(notification).throwEvent(with(equal("K1")), with(equal(caller)), with(equal(DigitalCollection.OBJECT_TYPE)),
							with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalCollection.OBJECT_TYPE, "read"))), with(any(String.class)));
					inSequence(sequence);
				}
			});
			try {
				utx.begin();
				em.joinTransaction();
				core.readObject("K1");
				utx.commit();
				// TODO Should generate an exception
				// fail("This object is deleted, should give an exception !!");
			} catch (Exception e) {
				// Normal
			}
			*/
			
			mockery.assertIsSatisfied();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	
	// @Test
	// public void testCRUDMetadata() {
	// final Sequence sequence = mockery.sequence("sequence1");
	// final Vector<Object> params = new Vector<Object>();
	// final Vector<Object> params2 = new Vector<Object>();
	//
	// // Metadata's target
	// String idTarget = UUID.randomUUID().toString();
	// final RegistryEntry entryTarget = new RegistryEntry("T1", new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, idTarget));
	//
	// try {
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("T1")));
	// will(returnValue(entryTarget));
	//
	// oneOf(binary).put(with(any(InputStream.class)));
	// will(returnValue("sha1-object1"));
	// inSequence(sequence);
	// oneOf(binary).size(with(equal("sha1-object1")));
	// will(returnValue(27l));
	// inSequence(sequence);
	// oneOf(binary).type(with(equal("sha1-object1")));
	// will(returnValue("text/plain"));
	// inSequence(sequence);
	//
	// oneOf(registry).create(with(equal("K1")), with(any(OrtolangObjectIdentifier.class)));
	// will(saveParams(params));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.AUTHOR)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.OWNER)), with(any(String.class)));
	// inSequence(sequence);
	//
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "create"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// // Create a metadata for the target
	// core.createMetadata("K1", "Name1", "A little bit of sample data".getBytes(), "T1");
	//
	// final RegistryEntry entry = new RegistryEntry("K1", (OrtolangObjectIdentifier) params.elementAt(1));
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// DigitalMetadata object = core.getMetadata("K1");
	// assertEquals("K1", object.getKey());
	// assertEquals("Name1", object.getName());
	// assertEquals("text/plain", object.getContentType());
	// assertEquals(27, object.getSize());
	// assertEquals("T1", object.getTarget());
	// assertTrue(object.getStream().equals("sha1-object1"));
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(binary).get(with(equal("sha1-object1")));
	// will(returnValue(new ByteArrayInputStream("A little bit of sample data".getBytes())));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read-data"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// byte[] data = core.getMetadataData("K1");
	// assertEquals(27, data.length);
	//
	// String idTarget2 = UUID.randomUUID().toString();
	// final RegistryEntry entryTarget2 = new RegistryEntry("T2", new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, DigitalReference.OBJECT_TYPE, idTarget2));
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	//
	// oneOf(registry).read(with(equal("T2")));
	// will(returnValue(entryTarget2));
	// inSequence(sequence);
	//
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	//
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "update"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// core.updateMetadata("K1", "Name11", "T2");
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// object = core.getMetadata("K1");
	// assertEquals("K1", object.getKey());
	// assertEquals("Name11", object.getName());
	// assertEquals("text/plain", object.getContentType());
	// assertEquals(27, object.getSize());
	// assertEquals("T2", object.getTarget());
	// assertTrue(object.getStream().equals("sha1-object1"));
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	//
	// oneOf(binary).put(with(any(InputStream.class)));
	// will(returnValue("sha1-object2"));
	// inSequence(sequence);
	// oneOf(binary).size(with(equal("sha1-object2")));
	// will(returnValue(28l));
	// inSequence(sequence);
	// oneOf(binary).type(with(equal("sha1-object2")));
	// will(returnValue("text/xml"));
	// inSequence(sequence);
	//
	// oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	//
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "update"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// core.updateMetadata("K1", "Name11", "A little bit of sample data".getBytes());
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// object = core.getMetadata("K1");
	// assertEquals("K1", object.getKey());
	// assertEquals("Name11", object.getName());
	// assertEquals("text/xml", object.getContentType());
	// assertEquals(28, object.getSize());
	// assertEquals("T2", object.getTarget());
	// assertTrue(object.getStream().equals("sha1-object2"));
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(registry).create(with(equal("K2")), with(any(OrtolangObjectIdentifier.class)), with(equal("K1")));
	// will(saveParams(params2));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.AUTHOR)), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.OWNER)), with(any(String.class)));
	// inSequence(sequence);
	//
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "clone"))), with(any(String.class)));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K2")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "create"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// core.cloneMetadata("K2", "K1");
	//
	// final RegistryEntry entryClone = new RegistryEntry("K2", (OrtolangObjectIdentifier) params2.elementAt(1));
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K2")));
	// will(returnValue(entryClone));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K2")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// DigitalMetadata clone = core.getMetadata("K2");
	// assertEquals("K2", clone.getKey());
	// assertEquals("Name11", clone.getName());
	// assertEquals("text/xml", clone.getContentType());
	// assertEquals(28, clone.getSize());
	// assertEquals("T2", clone.getTarget());
	// assertTrue(clone.getStream().equals("sha1-object2"));
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(registry).delete(with(equal("K1")));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "delete"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// core.deleteMetadata("K1");
	//
	// mockery.checking(new Expectations() {
	// {
	// oneOf(registry).read(with(equal("K1")));
	// will(returnValue(entry));
	// inSequence(sequence);
	// oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalMetadata.OBJECT_TYPE)),
	// with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalMetadata.OBJECT_TYPE, "read"))), with(any(String.class)));
	// inSequence(sequence);
	// }
	// });
	// try {
	// core.getMetadata("K1");
	// //TODO Should generate an exception
	// // fail("This object is deleted, should give an exception !!");
	// } catch ( Exception e ) {
	// //Normal
	// }
	//
	//
	// mockery.assertIsSatisfied();
	// } catch ( Exception e ) {
	// logger.log(Level.SEVERE, e.getMessage(), e);
	// fail(e.getMessage());
	// }
	// }

}
