package fr.ortolang.diffusion.runtime.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public abstract class RuntimeEngineTask implements JavaDelegate {
	
	private static final Logger logger = Logger.getLogger(RuntimeEngineTask.class.getName());
	
	private UserTransaction userTx;
	private RuntimeEngine engine;
	private RuntimeService runtime;
	private MembershipService membership;
	private BinaryStoreService store;
	private CoreService core;
	private BrowserService browser;
	private RegistryService registry;
	private PublicationService publication;
	
	public MembershipService getMembershipService() throws RuntimeEngineTaskException {
		try {
			if (membership == null) {
				membership = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
			}
			return membership;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	public BinaryStoreService getBinaryStore() throws RuntimeEngineTaskException {
		try {
			if (store == null) {
				store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME);
			}
			return store;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public CoreService getCoreService() throws RuntimeEngineTaskException {
		try {
			if (core == null) {
				core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
			}
			return core;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public RuntimeService getRuntimeService() throws RuntimeEngineTaskException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	public BrowserService getBrowserService() throws RuntimeEngineTaskException {
		try {
			if (browser == null) {
				browser = (BrowserService) OrtolangServiceLocator.findService(BrowserService.SERVICE_NAME);
			}
			return browser;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	public RegistryService getRegistryService() throws RuntimeEngineTaskException {
		try {
			if (registry == null) {
				registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
			}
			return registry;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	public PublicationService getPublicationService() throws RuntimeEngineTaskException {
		try {
			if (publication == null) {
				publication = (PublicationService) OrtolangServiceLocator.findService("publication");
			}
			return publication;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	public UserTransaction getUserTransaction() throws RuntimeEngineTaskException {
		try {
			if (userTx == null) {
				userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
			}
			return userTx;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	private RuntimeEngine getRuntimeEngine() throws RuntimeEngineTaskException {
		try {
			if (engine == null) {
				engine = (RuntimeEngine) OrtolangServiceLocator.lookup(RuntimeEngine.SERVICE_NAME);
			}
			return engine;
		} catch ( Exception e ) {
			throw new RuntimeEngineTaskException(e);
		}
	}
	
	@Override
	public void execute(DelegateExecution execution) {
		try {
			try {
				logger.log(Level.INFO, "Starting RuntimeTask execution");
				LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
				if ( needEngineAuth() ) {
					logger.log(Level.FINE, "Loggin needed, trying to authenticate engine");
					loginContext.login();
				}
				try {
					logger.log(Level.FINE, "Loggin ok, sending events of process evolution");
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityStartEvent(execution.getProcessBusinessKey(), getTaskName(), "service task: " + execution.getCurrentActivityName() + " started"));
					
					try {
						logger.log(Level.FINE, "Executing task");
						executeTask(execution);
						logger.log(Level.FINE, "Task executed");
					} catch ( RuntimeEngineTaskException e ) {
						logger.log(Level.SEVERE, "Task execution error", e);
						throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityErrorEvent(execution.getProcessBusinessKey(), getTaskName(), "service task: " + execution.getCurrentActivityName() + " error " + e.getMessage()));
					}
					
					logger.log(Level.FINE, "Sending events of process evolution");
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityCompleteEvent(execution.getProcessBusinessKey(), getTaskName(), "service task: " + execution.getCurrentActivityName() + " completed"));
				} finally {
					if ( needEngineAuth() ) {
						logger.log(Level.FINE, "Engine authenticated, trying to logout");
						loginContext.logout();
					}
				}
			} catch ( LoginException e ) {
				logger.log(Level.SEVERE, "Abstract Runtime Task login error", e);
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityErrorEvent(execution.getProcessBusinessKey(), getTaskName(), "service task: " + execution.getCurrentActivityName() + " error" + e.getMessage()));
			}
		} catch ( RuntimeEngineTaskException e ) {
			logger.log(Level.SEVERE, "Abstract Runtime Task error", e);
		}
	}
	
	public void throwRuntimeEngineEvent(RuntimeEngineEvent event) throws RuntimeEngineTaskException {
		try {
			getRuntimeEngine().notify(event);
		} catch (RuntimeEngineException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to throw event", e);
		}
	}
	
	public abstract String getTaskName();
	
	public abstract boolean needEngineAuth();
	
	public abstract void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException ;

}
