package fr.ortolang.diffusion.bootstrap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.shell.ShellServiceBean;

@Startup
@Singleton(name = BootstrapService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RunAs("user")
@RunAsPrincipal(value=MembershipService.SUPERUSER_IDENTIFIER)
public class BootstrapServiceBean implements BootstrapService {
	
	private static Logger logger = Logger.getLogger(ShellServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private CoreService core;
	@Resource
	private SessionContext ctx;
	
	public BootstrapServiceBean() {
		logger.log(Level.FINE, "new bootstrap service instance created");
	}

	@PostConstruct
	public void init() throws Exception {
		bootstrap();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void bootstrap() throws BootstrapServiceException {
		logger.log(Level.INFO, "checking bootstrap status");
		try {
			registry.lookup(BootstrapService.BOOTSTRAP_KEY);
			logger.log(Level.INFO, "bootstrap key found, nothing to do.");
		} catch ( KeyNotFoundException e ) {
			try {
				logger.log(Level.INFO, "bootstrap key not found, bootstraping plateform...");
				
				Map<String, List<String>> guestReadRules = new HashMap<String, List<String>>();
	            guestReadRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList(new String[] {"read"}));
	            
				logger.log(Level.FINE, "creating root profile");
	            membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super User", "root@ortolang.org", ProfileStatus.ACTIVATED);
	            
	            logger.log(Level.FINE, "creating guest profile");
	            membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Guest", "guest@ortolang.org", ProfileStatus.ACTIVATED);
	            logger.log(Level.FINE, "change owner of guest profile to root and set guest read rules");
	            authorisation.updatePolicyOwner(MembershipService.UNAUTHENTIFIED_IDENTIFIER, MembershipService.SUPERUSER_IDENTIFIER);
	            authorisation.setPolicyRules(MembershipService.UNAUTHENTIFIED_IDENTIFIER, guestReadRules);
	            
	            logger.log(Level.FINE, "creating moderators group");
	            membership.createGroup(MembershipService.MODERATOR_GROUP_KEY, "Publication Moderators", "Users that have the ability to publich some keys");
	            logger.log(Level.FINE, "set guest read rules");
	            authorisation.setPolicyRules(MembershipService.MODERATOR_GROUP_KEY, guestReadRules);
	            
	            logger.log(Level.FINE, "create bootstrap file");
	            Properties props = new Properties();
                props.setProperty("bootstrap.status", "done");
                props.setProperty("bootstrap.timestamp", System.currentTimeMillis() + "");
                props.setProperty("bootstrap.version", BootstrapService.VERSION);
	            core.createDataObject(BootstrapService.BOOTSTRAP_KEY, "bootstrap.txt", "bootstrap file", props.toString().getBytes());
	            
	            logger.log(Level.INFO, "bootstrap done.");
			} catch (MembershipServiceException | ProfileAlreadyExistsException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException | AccessDeniedException e1) {
				ctx.setRollbackOnly();
				throw new BootstrapServiceException("unable to check plateform bootstrap status", e);
			}
		} catch ( RegistryServiceException e ) {
			throw new BootstrapServiceException("unable to check plateform bootstrap status", e);
		}
	}

}
