package fr.ortolang.diffusion.runtime.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public abstract class RuntimeEngineTask implements JavaDelegate {

	public static final String BAG_PATH_PARAM_NAME = "bagpath";
	public static final String BAG_VERSIONS_PARAM_NAME = "bagversions";
	public static final String BAG_VERSION_PARAM_NAME = "bagversion";

	public static final String METADATA_ENTRIES_PARAM_NAME = "metadataentries";
	public static final String OBJECT_ENTRIES_PARAM_NAME = "objectentries";

	public static final String ROOT_COLLECTION_PARAM_NAME = "root";
	public static final String SNAPSHOT_NAME_PARAM_NAME = "snapshot";

	public static final String WORKSPACE_KEY_PARAM_NAME = "wskey";
	public static final String WORKSPACE_NAME_PARAM_NAME = "wsname";
	public static final String WORKSPACE_TYPE_PARAM_NAME = "wstype";

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
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public BinaryStoreService getBinaryStore() throws RuntimeEngineTaskException {
		try {
			if (store == null) {
				store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME);
			}
			return store;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public CoreService getCoreService() throws RuntimeEngineTaskException {
		try {
			if (core == null) {
				core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
			}
			return core;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public RuntimeService getRuntimeService() throws RuntimeEngineTaskException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public BrowserService getBrowserService() throws RuntimeEngineTaskException {
		try {
			if (browser == null) {
				browser = (BrowserService) OrtolangServiceLocator.findService(BrowserService.SERVICE_NAME);
			}
			return browser;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public RegistryService getRegistryService() throws RuntimeEngineTaskException {
		try {
			if (registry == null) {
				registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
			}
			return registry;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public PublicationService getPublicationService() throws RuntimeEngineTaskException {
		try {
			if (publication == null) {
				publication = (PublicationService) OrtolangServiceLocator.findService("publication");
			}
			return publication;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public UserTransaction getUserTransaction() throws RuntimeEngineTaskException {
		try {
			if (userTx == null) {
				userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
			}
			return userTx;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	private RuntimeEngine getRuntimeEngine() throws RuntimeEngineTaskException {
		try {
			if (engine == null) {
				engine = (RuntimeEngine) OrtolangServiceLocator.lookup(RuntimeEngine.SERVICE_NAME);
			}
			return engine;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	@Override
	public void execute(DelegateExecution execution) {
		try {
			logger.log(Level.INFO, "Starting RuntimeTask execution");
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityStartEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " STARTED"));

			try {
				logger.log(Level.FINE, "Executing task");
				executeTask(execution);
				logger.log(Level.FINE, "Task executed");
			} catch (RuntimeEngineTaskException e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityErrorEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " IN ERROR: " + e.getMessage()));
				// TODO provide capability for task to say if it's needed to abort process on error ( task.abortProcessOnError():boolean )
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessAbortEvent(execution.getProcessBusinessKey(), e.getMessage()));
				throw e;
			}

			logger.log(Level.FINE, "Sending events of process evolution");
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityCompleteEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " COMPLETED"));
		} catch (RuntimeEngineTaskException e) {
			logger.log(Level.SEVERE, "Unexpected runtime task error, should result in inconsistent state of the workflow", e);
			throw new BpmnError("RuntimeTaskExecutionError", e.getMessage());
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

	// public abstract boolean abortProcessOnError();

	public abstract void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException;

}
