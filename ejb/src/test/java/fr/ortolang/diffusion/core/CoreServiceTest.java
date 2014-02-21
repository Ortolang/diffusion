package fr.ortolang.diffusion.core;

import static org.junit.Assert.fail;
import static fr.ortolang.diffusion.SaveParamsAction.saveParams;

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

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class CoreServiceTest {
	
	private Logger logger = Logger.getLogger(CoreServiceTest.class.getName());

	private Mockery mockery;
	private RegistryService registry;
	private BinaryStoreService binary;
	private NotificationService notification;
	private CoreService core;
	
	@Before
	public void setup() {
		logger.log(Level.INFO, "setting up test environment");
		try {
			mockery = new Mockery();
			registry = mockery.mock(RegistryService.class);
			binary = mockery.mock(BinaryStoreService.class);
			notification = mockery.mock(NotificationService.class);
			core = new CoreServiceBean();
			((CoreServiceBean)core).setRegistryService(registry);
			((CoreServiceBean)core).setBinaryStoreService(binary);
			((CoreServiceBean)core).setNotificationService(notification);
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
                    oneOf(registry).setProperty(with(equal("K1")), with(any(String.class)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(any(String.class)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(any(String.class)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(registry).setProperty(with(equal("K1")), with(any(String.class)), with(any(String.class)));
                    inSequence(sequence);
                    oneOf(notification).throwEvent(with(equal("K1")), with(any(String.class)), with(any(String.class)), with(equal("core.object.create")), with(any(String.class)));
                    inSequence(sequence);
                }
            });
			core.createObject("K1", "Name1", "This is the object one", "A little bit of sample data".getBytes());
			
			mockery.assertIsSatisfied();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	
	

}
