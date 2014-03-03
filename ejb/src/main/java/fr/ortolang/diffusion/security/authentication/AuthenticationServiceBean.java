package fr.ortolang.diffusion.security.authentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Local(AuthenticationService.class)
@Stateless(name = AuthenticationService.SERVICE_NAME)
public class AuthenticationServiceBean implements AuthenticationService {
	
	private Logger logger = Logger.getLogger(AuthenticationServiceBean.class.getName());
	
	@Resource
	private SessionContext ctx;

	@Override
	public String getConnectedIdentifier() {
		logger.log(Level.FINE, "getting connected identifier");
		return ctx.getCallerPrincipal().getName();
	}

}
