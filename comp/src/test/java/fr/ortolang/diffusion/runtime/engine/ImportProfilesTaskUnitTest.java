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
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.engine.task.ImportProfilesTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ImportProfilesTaskUnitTest {

	@Test
	public void tesImportProfilesTask() throws RuntimeEngineTaskException, RuntimeEngineException, RegistryServiceException, KeyNotFoundException, CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, MembershipServiceException, ProfileAlreadyExistsException {
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
		
		
		ImportProfilesTask task = new ImportProfilesTask();
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
			allowing(execution).hasVariable("profilespath"); will(returnValue(true));
			allowing(execution).getVariable("profilespath", String.class); will(returnValue(this.getClass().getClassLoader().getResource("profiles_lite.json").getPath()));
			allowing(execution).hasVariable("profilesoverwrites"); will(returnValue(true));
			allowing(execution).getVariable("profilesoverwrites", String.class); will(returnValue("true"));
			allowing(membership).createProfile(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(ProfileStatus.ACTIVE));
			allowing(membership).setProfileInfo(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(ProfileDataVisibility.EVERYBODY), with(ProfileDataType.STRING), with(any(String.class)));
			allowing(execution).getProcessBusinessKey(); will(returnValue("PID"));
			allowing(engine).notify(with(any(RuntimeEngineEvent.class)));
	    }});
		
		task.executeTask(execution);
	}

}
