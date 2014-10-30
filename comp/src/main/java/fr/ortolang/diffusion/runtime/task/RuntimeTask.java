package fr.ortolang.diffusion.runtime.task;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public abstract class RuntimeTask implements JavaDelegate {
	
	private static final Logger logger = Logger.getLogger(RuntimeTask.class.getName());
	
	private UserTransaction userTx;
	private RuntimeService runtime;
	private MembershipService membership;
	private BinaryStoreService store;
	private CoreService core;
	private FixedValue runas = null;
	
	public FixedValue getRunas() {
		return runas;
	}

	public void setRunas(FixedValue runas) {
		this.runas = runas;
	}
	
	public MembershipService getMembershipService() throws RuntimeTaskException {
		try {
			if (membership == null) {
				membership = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
			}
			return membership;
		} catch ( Exception e ) {
			throw new RuntimeTaskException(e);
		}
	}
	
	public BinaryStoreService getBinaryStore() throws RuntimeTaskException {
		try {
			if (store == null) {
				store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME);
			}
			return store;
		} catch ( Exception e ) {
			throw new RuntimeTaskException(e);
		}
	}

	public CoreService getCoreService() throws RuntimeTaskException {
		try {
			if (core == null) {
				core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
			}
			return core;
		} catch ( Exception e ) {
			throw new RuntimeTaskException(e);
		}
	}

	public RuntimeService getRuntimeService() throws RuntimeTaskException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch ( Exception e ) {
			throw new RuntimeTaskException(e);
		}
	}
	
	public UserTransaction getUserTransaction() throws RuntimeTaskException {
		try {
			if (userTx == null) {
				userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
			}
			return userTx;
		} catch ( Exception e ) {
			throw new RuntimeTaskException(e);
		}
	}
	
	@Override
	public void execute(DelegateExecution execution) {
		try {
			LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext(OrtolangConfig.getInstance().getProperty("runtime.engine.login"), OrtolangConfig.getInstance().getProperty("runtime.engine.password"));
			loginContext.login();
			try {
				//TODO check if this could be done using event 
				getUserTransaction().begin();
				getRuntimeService().updateProcessActivity(execution.getProcessBusinessKey(), getTaskName());
				getRuntimeService().appendProcessLog(execution.getProcessBusinessKey(), "[SERVICE TASK] " + getTaskName() + " started on " + new Date());
				getUserTransaction().commit();
				
				try {
					if ( runas != null ) {
						execute(runas.getExpressionText(), execution);
					} else {
						execute(null, execution);
					}
				} catch ( Exception e ) {
					getUserTransaction().begin();
					getRuntimeService().appendProcessLog(execution.getProcessBusinessKey(), "[SERVICE TASK] " + getTaskName() + " unexpected error while executing task : " + e.getMessage());
					getUserTransaction().commit();
				}
				
				getUserTransaction().begin();
				getRuntimeService().appendProcessLog(execution.getProcessBusinessKey(), "[SERVICE TASK] " + getTaskName() + " ended on " + new Date());
				getUserTransaction().commit();
			} catch (RuntimeTaskException | NotSupportedException | SystemException | RuntimeServiceException | SecurityException | IllegalStateException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
				//TODO maybe send en event 
				logger.log(Level.WARNING, "error while executing runtime task", e);
			} finally {
				loginContext.logout();
			}
		} catch ( LoginException e ) {
			//TODO
		}
	}
	
	public abstract String getTaskName();
	
	public abstract void execute(String runas, DelegateExecution execution) throws RuntimeTaskException ;

}
