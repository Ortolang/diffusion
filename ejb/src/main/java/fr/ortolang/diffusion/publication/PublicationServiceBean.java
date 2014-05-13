package fr.ortolang.diffusion.publication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;


@Remote(PublicationService.class)
@Stateless(name = PublicationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class PublicationServiceBean implements PublicationService {

	private Logger logger = Logger.getLogger(PublicationServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexingService indexing;
	@EJB
	private NotificationService notification;
	@Resource
	private SessionContext ctx;
	
	public PublicationServiceBean() {
	}
	
	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registryService) {
		this.registry = registryService;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notification = notificationService;
	}

	public IndexingService getIndexingService() {
		return indexing;
	}

	public void setIndexingService(IndexingService indexing) {
		this.indexing = indexing;
	}

	public MembershipService getMembershipService() {
		return membership;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
	}
	
	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}

	@Override
	public void publish(Set<String> keys) throws PublicationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "publishing keys");
		try {
			//TODO l'appel à publish doit être fait par un modérateur
			// il faudra surement créer un groupe global à la plateforme (moderators) pour l'instant seul root peut publier
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			for ( String key : keys ) {
				if ( !registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.WAITING.name()) ) {
					throw new PublicationServiceException("unable to publish key [" + key + "] because key is not in state " + OrtolangObjectState.Status.WAITING.name());
				}
				Map<String, List<String>> rules = new HashMap<String, List<String>>();
				rules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read"));
				authorisation.updatePolicyOwner(key, MembershipService.SUPERUSER_IDENTIFIER);
				authorisation.setPolicyRules(key, rules);
				registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
				registry.lock(key, MembershipService.SUPERUSER_IDENTIFIER);
				registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
				registry.setProperty(key, OrtolangObjectProperty.PUBLICATION_TIMESTAMP, "" + System.currentTimeMillis());
				indexing.reindex(key);
				notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "publish"), "");
			}
		} catch (AuthorisationServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IndexingServiceException e ) {
			ctx.setRollbackOnly();
			throw new PublicationServiceException("error during submitting keys to publication : " + e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void submit(Set<String> keys) throws PublicationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "submiting keys for publication");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			for ( String key : keys ) {
				authorisation.checkOwnership(key, subjects);
				if ( !registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.DRAFT.value()) ) {
					throw new PublicationServiceException("unable to submit key [" + key + "] for publication because key is not in state " + OrtolangObjectState.Status.DRAFT.name());
				}
				if ( registry.getLock(key).length() > 0 ) {
					throw new PublicationServiceException("unable to submit key [" + key + "] for publication because key is locked");
				}
				registry.setPublicationStatus(key, OrtolangObjectState.Status.WAITING.value());
				registry.setProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
				registry.lock(key, MembershipService.SUPERUSER_IDENTIFIER);
				notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "submit-for-publication"), "");
			}
		} catch (AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException e ) {
			ctx.setRollbackOnly();
			throw new PublicationServiceException("error during submitting keys to publication : " + e);
		}
	}
	
	@Override
	public String getServiceName() {
		return PublicationService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return PublicationService.OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		return new String[] {};
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

}