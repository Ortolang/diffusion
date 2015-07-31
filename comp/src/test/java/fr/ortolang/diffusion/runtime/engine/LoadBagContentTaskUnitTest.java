package fr.ortolang.diffusion.runtime.engine;

import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.DelegateExecution;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.engine.task.LoadBagContentTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class LoadBagContentTaskUnitTest {
	
	@Test
	public void testLoadBagContentTask() throws RuntimeEngineTaskException, RuntimeEngineException, RegistryServiceException, KeyNotFoundException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		Mockery mockery = new Mockery();
		final UserTransaction userTx = mockery.mock(UserTransaction.class);
		final RegistryService registry = mockery.mock(RegistryService.class);
		final BinaryStoreService store = mockery.mock(BinaryStoreService.class);
		final RuntimeService runtime = mockery.mock(RuntimeService.class);
		final RuntimeEngine engine = mockery.mock(RuntimeEngine.class);
		final CoreService core = mockery.mock(CoreService.class);
		final MembershipService membership = mockery.mock(MembershipService.class);
		final BrowserService browser = mockery.mock(BrowserService.class);
		final PublicationService publication = mockery.mock(PublicationService.class);
		final DelegateExecution execution = mockery.mock(DelegateExecution.class);
		
		
		LoadBagContentTask task = new LoadBagContentTask();
		task.userTx = userTx;
		task.registry = registry;
		task.store = store;
		task.runtime = runtime;
		task.engine = engine;
		task.core = core;
		task.membership = membership;
		task.browser = browser;
		task.publication = publication;
		
		
		mockery.checking(new Expectations() {{
			allowing(execution).hasVariable("bagpath"); will(returnValue(true));
			allowing(execution).getVariable("bagpath", String.class); will(returnValue(this.getClass().getClassLoader().getResource("test_bag").getPath()));
			allowing(execution).getProcessBusinessKey(); will(returnValue("PID"));
			allowing(engine).notify(with(any(RuntimeEngineEvent.class)));
			allowing(execution).setVariable(with(any(String.class)), with(any(String.class)));
	    }});
		
		task.executeTask(execution);
	}

}
