package fr.ortolang.diffusion.runtime.task;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authentication.AuthenticationLoginContextFactory;

public class HelloWorldTask implements JavaDelegate {

	private static final Logger logger = Logger.getLogger(HelloWorldTask.class.getName());

	private MembershipService membership;
	private FixedValue runas;
	private FixedValue delay;

	public HelloWorldTask() {
		logger.log(Level.INFO, "New HelloWorldTask class instanciated");
		delay = null;
		runas = null;
	}

	public MembershipService getMembershipService() throws OrtolangException {
		if (membership == null) {
			membership = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
		}
		return membership;
	}

	protected void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public FixedValue getDelay() {
		return delay;
	}

	public void setDelay(FixedValue delay) {
		this.delay = delay;
	}
	
	public FixedValue getRunas() {
		return runas;
	}

	public void setRunas(FixedValue runas) {
		this.runas = runas;
	}

	@Override
	public void execute(DelegateExecution execution) throws LoginException {
		logger.log(Level.INFO, "Executing HelloWorldTask");
		LoginContext loginContext = AuthenticationLoginContextFactory.createLoginContext("activiti", "activiti54");
		loginContext.login();
		try {
			String name = (String) execution.getVariable("name");
			if (delay != null) {
				logger.log(Level.INFO, "sleeping a while...");
				try {
					Thread.sleep(Integer.parseInt(delay.getExpressionText()));
				} catch (NumberFormatException | InterruptedException e) {
					logger.log(Level.WARNING, "error while waiting...", e);
				}
			}
			String caller = getMembershipService().getProfileKeyForConnectedIdentifier();
			logger.log(Level.INFO, caller + " is saying hello to " + name);
			execution.setVariable("greettime", new Date());
		} catch (OrtolangException e) {
			logger.log(Level.WARNING, "error while waiting...", e);
		} finally {
			loginContext.logout();
		}
	}

}
