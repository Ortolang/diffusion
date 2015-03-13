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
import fr.ortolang.diffusion.runtime.engine.task.ImportWorkspaceTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ImportWorkspaceTaskUnitTest {
	
	@Test
	public void testImportWorkspaceTask() throws RuntimeEngineTaskException, RuntimeEngineException, RegistryServiceException, KeyNotFoundException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
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
		
		
		ImportWorkspaceTask task = new ImportWorkspaceTask();
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
			allowing(execution).hasVariable("wskey"); will(returnValue(true));
			allowing(execution).getVariable("wskey", String.class); will(returnValue("WSK1"));
			allowing(execution).hasVariable("wsname"); will(returnValue(true));
			allowing(execution).getVariable("wsname", String.class); will(returnValue("Workspace Test"));
			allowing(execution).hasVariable("wstype"); will(returnValue(true));
			allowing(execution).getVariable("wstype", String.class); will(returnValue("test"));
			allowing(execution).getVariable("wsalias", String.class); will(returnValue(null));
			allowing(execution).getProcessBusinessKey(); will(returnValue("pid1"));
			allowing(engine).notify(with(any(RuntimeEngineEvent.class)));
			allowing(registry).lookup("WSK1"); will(throwException(new KeyNotFoundException("WSK1 does not exists")));
			oneOf(core).createWorkspace("WSK1", "Workspace Test", "test");
	    }});
		
		task.executeTask(execution);
	}

}
