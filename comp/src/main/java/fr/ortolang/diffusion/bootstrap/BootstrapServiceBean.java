package fr.ortolang.diffusion.bootstrap;

import java.io.ByteArrayInputStream;
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
import fr.ortolang.diffusion.core.InvalidPathException;
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
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.tool.ToolService;
import fr.ortolang.diffusion.tool.ToolServiceException;

@Startup
@Singleton(name = BootstrapService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RunAs("user")
@RunAsPrincipal(value = MembershipService.SUPERUSER_IDENTIFIER)
public class BootstrapServiceBean implements BootstrapService {

	private static Logger logger = Logger.getLogger(BootstrapServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private CoreService core;
	@EJB
	private ToolService tool;
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
		logger.log(Level.INFO, "checking bootstrap status...");
		try {
			registry.lookup(BootstrapService.WORKSPACE_KEY);
			logger.log(Level.INFO, "bootstrap key found, nothing to do.");
		} catch (KeyNotFoundException e) {
			try {
				logger.log(Level.INFO, "bootstrap key not found, bootstraping plateform...");

				Map<String, List<String>> guestReadRules = new HashMap<String, List<String>>();
				guestReadRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList(new String[] { "read" }));

				logger.log(Level.FINE, "creating root profile");
				membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super User", "root@ortolang.org", ProfileStatus.ACTIVATED);

				logger.log(Level.FINE, "creating guest profile");
				membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Guest", "guest@ortolang.org", ProfileStatus.ACTIVATED);
				logger.log(Level.FINE, "change owner of guest profile to root and set guest read rules");
				authorisation.updatePolicyOwner(MembershipService.UNAUTHENTIFIED_IDENTIFIER, MembershipService.SUPERUSER_IDENTIFIER);
				authorisation.setPolicyRules(MembershipService.UNAUTHENTIFIED_IDENTIFIER, guestReadRules);

				logger.log(Level.FINE, "creating moderators group");
				membership.createGroup(MembershipService.MODERATOR_GROUP_KEY, "Publication Moderators", "Moderators of the plateform can publish content");
				membership.addMemberInGroup(MembershipService.MODERATOR_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
				logger.log(Level.FINE, "set guest read rules");
				authorisation.setPolicyRules(MembershipService.MODERATOR_GROUP_KEY, guestReadRules);

				logger.log(Level.FINE, "create system workspace");
				core.createWorkspace(BootstrapService.WORKSPACE_KEY, "System Workspace", "system");
				Properties props = new Properties();
				props.setProperty("bootstrap.status", "done");
				props.setProperty("bootstrap.timestamp", System.currentTimeMillis() + "");
				props.setProperty("bootstrap.version", BootstrapService.VERSION);
				String hash = core.put(new ByteArrayInputStream(props.toString().getBytes()));
				core.createDataObject(BootstrapService.WORKSPACE_KEY, "/bootstrap.txt", "bootstrap file", hash);
				
				logger.log(Level.FINE, "declare tools");
				tool.declareTool("treetagger", "TreeTagger", 
				"A language independent part-of-speech tagger\nThe TreeTagger is a tool for annotating text with part-of-speech and lemma information. "
				+ "It was developed by Helmut Schmid in the TC project at the Institute for Computational Linguistics of the University of Stuttgart. ",
				"See http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/ or http://www.tal.univ-paris3.fr/cours/BAO-master/treetagger-win32/README-treetagger.txt", 
				"fr.ortolang.diffusion.tool.treetagger.TreeTaggerInvokerTest");

				logger.log(Level.INFO, "bootstrap done.");
			} catch (MembershipServiceException | ProfileAlreadyExistsException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException
					| AccessDeniedException | KeyNotFoundException | InvalidPathException | DataCollisionException | ToolServiceException e1) {
				ctx.setRollbackOnly();
				throw new BootstrapServiceException("unable to bootstrap plateform", e1);
			}
		} catch (RegistryServiceException e) {
			throw new BootstrapServiceException("unable to check plateform bootstrap status", e);
		}
	}

}
