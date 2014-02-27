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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.core.entity.DigitalObject;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class CoreServiceTest {
	
	private Logger logger = Logger.getLogger(CoreServiceTest.class.getName());

	private Mockery mockery;
	private RegistryService registry;
	private BinaryStoreService binary;
	private NotificationService notification;
	private IndexingService indexing;
	private CoreService core;
	
	@Before
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
		try {
			mockery = new Mockery();
			registry = mockery.mock(RegistryService.class);
			binary = mockery.mock(BinaryStoreService.class);
			notification = mockery.mock(NotificationService.class);
			indexing = mockery.mock(IndexingService.class);
			core = new CoreServiceBean();
			((CoreServiceBean)core).setRegistryService(registry);
			((CoreServiceBean)core).setBinaryStoreService(binary);
			((CoreServiceBean)core).setNotificationService(notification);
			((CoreServiceBean)core).setIndexingService(indexing);
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
		final Sequence sequence = mockery.sequence("sequence1");
		final Vector<Object> params = new Vector<Object>();
		final Vector<Object> params2 = new Vector<Object>();
		
		try {
			mockery.checking(new Expectations() {
                {
                    oneOf(binary).put(with(any(InputStream.class)));
                    will(returnValue("sha1-object1"));
                    inSequence(sequence);
                    oneOf(binary).size(with(equal("sha1-object1")));
                    will(returnValue(27l));
                    inSequence(sequence);
                    oneOf(binary).type(with(equal("sha1-object1")));
                    will(returnValue("text/plain"));
                    inSequence(sequence);
                    oneOf(registry).create(with(equal("K1")), with(any(OrtolangObjectIdentifier.class)));
                    will(saveParams(params));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.AUTHOR)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(equal(OrtolangObjectProperty.OWNER)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(indexing).index(with(equal("K1")));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			core.createObject("K1", "Name1", "This is the object one", "A little bit of sample data".getBytes());
			
			final RegistryEntry entry = new RegistryEntry("K1", (OrtolangObjectIdentifier) params.elementAt(1));
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			DigitalObject object = core.getObject("K1");
			assertEquals("K1", object.getKey());
			assertEquals("Name1", object.getName());
			assertEquals("This is the object one", object.getDescription());
			assertEquals("text/plain", object.getContentType());
			assertEquals(27, object.getSize());
			assertEquals(0, object.getNbReads());
			assertTrue(object.getStreams().containsValue("sha1-object1"));
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(binary).get(with(equal("sha1-object1")));
                    will(returnValue(new ByteArrayInputStream("A little bit of sample data".getBytes())));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read-data"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			byte[] data = core.getObjectData("K1");
			assertEquals(27, data.length);
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(indexing).reindex(with(equal("K1")));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "update"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			core.updateObject("K1", "Name11", "This is the object one updated");
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			object = core.getObject("K1");
			assertEquals("K1", object.getKey());
			assertEquals("Name11", object.getName());
			assertEquals("This is the object one updated", object.getDescription());
			assertEquals(1, object.getNbReads());
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(registry).create(with(equal("K2")), with(any(OrtolangObjectIdentifier.class)), with(equal("K1")));
                    will(saveParams(params2));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.CREATION_TIMESTAMP)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.AUTHOR)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K2")), with(equal(OrtolangObjectProperty.OWNER)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(indexing).index(with(equal("K2")));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "clone"))), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K2")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "create"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			core.cloneObject("K2", "K1");
			
			final RegistryEntry entryClone = new RegistryEntry("K2", (OrtolangObjectIdentifier) params2.elementAt(1));
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K2")));
                    will(returnValue(entryClone));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K2")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			DigitalObject clone = core.getObject("K2");
			assertEquals("K2", clone.getKey());
			assertEquals("Name11", clone.getName());
			assertEquals("This is the object one updated", clone.getDescription());
			assertEquals("text/plain", clone.getContentType());
			assertEquals(27, clone.getSize());
			assertEquals(0, clone.getNbReads());
			assertTrue(clone.getStreams().containsValue("sha1-object1"));
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entryClone));
                    inSequence(sequence);
                    oneOf(registry).delete(with(equal("K1")));
                    inSequence(sequence);
                    oneOf(indexing).remove(with(equal("K1")));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "delete"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			core.deleteObject("K1");
			
			
			mockery.checking(new Expectations() {
                {
                    oneOf(registry).lookup(with(equal("K1")));
                    will(returnValue(entry));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(equal(DigitalObject.OBJECT_TYPE)), with(equal(OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DigitalObject.OBJECT_TYPE, "read"))), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			try {
				core.getObject("K1");
				//TODO Should generate an exception
				//fail("This object is deleted, should give an exception !!");
			} catch ( Exception e ) {
				//Normal
			}
			
			mockery.assertIsSatisfied();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	
	

}
