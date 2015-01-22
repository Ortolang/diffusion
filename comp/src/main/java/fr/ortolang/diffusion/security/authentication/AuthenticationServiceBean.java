package fr.ortolang.diffusion.security.authentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;


@Local(AuthenticationService.class)
@Stateless(name = AuthenticationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class AuthenticationServiceBean implements AuthenticationService {
	
	private Logger logger = Logger.getLogger(AuthenticationServiceBean.class.getName());
	
	@Resource
	private SessionContext ctx;

	@Override
	public String getConnectedIdentifier() {
		logger.log(Level.FINE, "Connected identifier " + ctx.getCallerPrincipal().getName());
		return ctx.getCallerPrincipal().getName();
	}

}
