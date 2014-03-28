package fr.ortolang.diffusion.browser;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Remote(BrowserService.class)
@Stateless(name = BrowserService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class BrowserServiceBean implements BrowserService {

	private Logger logger = Logger.getLogger(BrowserServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;

	public BrowserServiceBean() {
	}

	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registry) {
		this.registry = registry;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notification) {
		this.notification = notification;
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

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectIdentifier lookup(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "looking up identifier for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "lookup"), "");
			return identifier;
		} catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("unable to lookup identifier for key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> list(int offset, int limit, String service, String type) throws BrowserServiceException {
		logger.log(Level.INFO, "listing keys");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> keys = registry.list(offset, limit, OrtolangObjectIdentifier.buildFilterPattern(service, type));
			notification.throwEvent("666", caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "list"), "");
			return keys;
		} catch (RegistryServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during listing keys", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long count(String service, String type) throws BrowserServiceException {
		logger.log(Level.INFO, "counting keys");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			long num = registry.count(OrtolangObjectIdentifier.buildFilterPattern(service, type));
			notification.throwEvent("666", caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "count"), "");
			return num;
		} catch (RegistryServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during couting identifiers", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing properties for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			List<OrtolangObjectProperty> properties = registry.getProperties(key); 
			notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "list-properties"), "");
			return properties;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during listing entries", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting property with name [" + name + "] for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			String value = registry.getProperty(key, name);
			OrtolangObjectProperty property = new OrtolangObjectProperty(name, value);
			notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "get-property"), "name=" + name);
			return property;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during getting property", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting state for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			boolean hidden = registry.isHidden(key);
			boolean deleted = registry.isDeleted(key);
			String lock = registry.getLock(key);
			String status = registry.getPublicationStatus(key);
			OrtolangObjectState state = new OrtolangObjectState(deleted, hidden, lock, status);
			notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "get-state"), "");
			return state;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during getting property", e);
		}
	}

	@Override
	public String getServiceName() {
		return BrowserService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return BrowserService.OBJECT_TYPE_LIST;
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
