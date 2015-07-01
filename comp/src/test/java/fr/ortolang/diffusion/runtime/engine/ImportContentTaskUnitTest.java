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
import fr.ortolang.diffusion.runtime.engine.task.ImportContentTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ImportContentTaskUnitTest {
	
	@Test
	public void tesImportContentTask() throws RuntimeEngineTaskException, RuntimeEngineException, RegistryServiceException, KeyNotFoundException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
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
		
		
		ImportContentTask task = new ImportContentTask();
		task.userTx = userTx;
		task.registry = registry;
		task.store = store;
		task.runtime = runtime;
		task.engine = engine;
		task.core = core;
		task.membership = membership;
		task.browser = browser;
		task.publication = publication;
		
		final String operations = "create-object	data/snapshots/1/objects/o1.txt	2b141ff6e12af000c045d5cb05a8ebd2d4e62e41	/o1.txt\r\n" +
						"create-object	data/snapshots/1/objects/a/b/oab1.txt	7dfac70c9d61453d1506c6104338c28449d14b6f	/a/b/oab1.txt\r\n" +
						"create-object	data/snapshots/1/objects/a/oa1.txt	964bc67ade85531675e9c3b999ad0d15a4522801	/a/oa1.txt\r\n" +
						"create-metadata	data/snapshots/1/metadata/md1.txt	/md1.txt\r\n" +
						"snapshot-workspace\r\n" +
						"create-object	data/snapshots/2/objects/b/c/d/obcd1.txt	4ad279e6ec016823e1186210afa37ba37d9d236b	/b/c/d/obcd1.txt\r\n" +
						"snapshot-workspace\r\n" +
						"create-object	data/snapshots/3/objects/a/b/oab2.txt	85d55c09649c78ea612c488e77e1ef164b2e15ec	/a/b/oab2.txt\r\n" +
						"create-object	data/snapshots/3/objects/a/oa2.txt	964bc67ade85531675e9c3b999ad0d15a4522801	/a/oa2.txt\r\n" +
						"create-object	data/snapshots/3/objects/b/cece/d/obcd1.txt	4ad279e6ec016823e1186210afa37ba37d9d236b	/b/cece/d/obcd1.txt\r\n" +
						"create-object	data/snapshots/3/objects/o2.txt	6f70ce2a783986fe58c0cc370e0c7114c812e04d	/o2.txt\r\n" +
						"update-object	data/snapshots/3/objects/a/oa1.txt	/a/oa1.txt\r\n" +
						"delete-object	data/snapshots/3/objects/o1.txt	/o1.txt\r\n" +
						"delete-object	data/snapshots/3/objects/a/b/oab1.txt	/a/b/oab1.txt\r\n" +
						"delete-object	data/snapshots/3/objects/b/c/d/obcd1.txt	/b/c/d/obcd1.txt\r\n" +
						"update-metadata	data/snapshots/3/metadata/md1.txt	/md1.txt\r\n" +
						"create-metadata	data/snapshots/3/metadata/a/oa1.txt/md1.txt	/a/oa1.txt/md1.txt";
		
		
		mockery.checking(new Expectations() {{
			allowing(execution).hasVariable("bagpath"); will(returnValue(true));
			allowing(execution).getVariable("bagpath", String.class); will(returnValue(this.getClass().getClassLoader().getResource("test_bag").getPath()));
			allowing(execution).hasVariable("operations"); will(returnValue(true));
			allowing(execution).getVariable("operations", String.class); will(returnValue(operations));
			allowing(execution).getProcessBusinessKey(); will(returnValue("PID"));
			allowing(engine).notify(with(any(RuntimeEngineEvent.class)));
			//allowing(execution).setVariable(with(any(String.class)), with(anything()));
	    }});
		
		task.executeTask(execution);
	}

}
