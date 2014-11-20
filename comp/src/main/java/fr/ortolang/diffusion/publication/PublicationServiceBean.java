package fr.ortolang.diffusion.publication;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.publication.type.PublicationType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Local(PublicationService.class)
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
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void publish(String key, PublicationType type) throws PublicationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "publishing key : " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			if ( !caller.equals(MembershipService.SUPERUSER_IDENTIFIER) && !subjects.contains(MembershipService.MODERATOR_GROUP_KEY) ) {
				throw new AccessDeniedException("user " + caller + " is not allowed to publish, only moderators can publish content");
			}
			
			if ( registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.PUBLISHED.value()) ) {
				logger.log(Level.WARNING, "key [" + key + "] is already published, nothing to do !!");
			} else {
				authorisation.updatePolicyOwner(key, MembershipService.SUPERUSER_IDENTIFIER);
				authorisation.setPolicyRules(key, type.getSecurityRules());
				registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
				registry.lock(key, MembershipService.SUPERUSER_IDENTIFIER);
				registry.update(key);
				indexing.index(key);
				notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "status"), "status=publish");
			}
		} catch (AuthorisationServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IndexingServiceException | MembershipServiceException e ) {
			logger.log(Level.SEVERE, "error during publication of key", e);
			ctx.setRollbackOnly();
			throw new PublicationServiceException("error during publishing key : " + e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void review(String key) throws PublicationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "submiting key for review");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			
			if ( registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.PUBLISHED.value()) ) {
				logger.log(Level.WARNING, "key [" + key + "] is already published, nothing to do !!");
			} else if ( registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.REVIEW.value()) ) {
				logger.log(Level.WARNING, "key [" + key + "] is already in review, nothing to do !!");
			} else {
				authorisation.checkOwnership(key, subjects);
				registry.setPublicationStatus(key, OrtolangObjectState.Status.REVIEW.value());
				registry.update(key);
				registry.lock(key, MembershipService.SUPERUSER_IDENTIFIER);
				notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "status"), "status=review");
			}
		} catch (AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException e ) {
			ctx.setRollbackOnly();
			throw new PublicationServiceException("error during submitting key for review : " + e);
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