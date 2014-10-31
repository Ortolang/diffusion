package fr.ortolang.diffusion.runtime.engine;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public abstract class RuntimeEngineTask implements JavaDelegate {
	
	private static final Logger logger = Logger.getLogger(RuntimeEngineTask.class.getName());
	
	private UserTransaction userTx;
	private RuntimeService runtime;
	private MembershipService membership;
	private BinaryStoreService store;
	private CoreService core;
	
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
	
	@Override
	public void execute(DelegateExecution execution) {
		try {
			logger.log(Level.INFO, "Starting abstract runtime task execution");
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				logger.log(Level.FINE, "Loggin ok, sending events of process evolution");
				ActivitiEvent event1 = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_ACTIVITY_UPDATE", getTaskName(), "", execution.getProcessBusinessKey(), "");
				execution.getEngineServices().getRuntimeService().dispatchEvent(event1);
				ActivitiEvent event2 = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_APPEND_LOG", getTaskName() + " started on " + new Date(), "", execution.getProcessBusinessKey(), "");
				execution.getEngineServices().getRuntimeService().dispatchEvent(event2);
				
				try {
					logger.log(Level.FINE, "Starting execution of concrete task");
					executeTask(execution);
					logger.log(Level.FINE, "Concrete Task executed");
				} catch ( Exception e ) {
					logger.log(Level.SEVERE, "Concrete Task execution error", e);
					ActivitiEvent event3 = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_APPEND_LOG", getTaskName() + " unexpected error while executing task : " + e.getMessage(), "", execution.getProcessBusinessKey(), "");
					execution.getEngineServices().getRuntimeService().dispatchEvent(event3);
				}
				
				logger.log(Level.FINE, "Sending events of process evolution");
				ActivitiEvent event3 = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_APPEND_LOG", getTaskName() + " ended on " + new Date(), "", execution.getProcessBusinessKey(), "");
				execution.getEngineServices().getRuntimeService().dispatchEvent(event3);
			} finally {
				loginContext.logout();
			}
		} catch ( LoginException e ) {
			logger.log(Level.SEVERE, "Abstract Runtime Task login error", e);
			ActivitiEvent event3 = ActivitiEventBuilder.createSignalEvent(ActivitiEventType.ACTIVITY_SIGNALED, "ORTOLANG", "PROCESS_APPEND_LOG", getTaskName() + " login error, unable to execute task", "", execution.getProcessBusinessKey(), "");
			execution.getEngineServices().getRuntimeService().dispatchEvent(event3);
		}
	}
	
	public abstract String getTaskName();
	
	public abstract void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException ;

}
