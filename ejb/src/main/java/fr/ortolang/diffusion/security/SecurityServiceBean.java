package fr.ortolang.diffusion.security;

import java.util.List;
import java.util.Map;
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
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
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

@Remote(SecurityService.class)
@Stateless(name = SecurityService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class SecurityServiceBean implements SecurityService {

	private Logger logger = Logger.getLogger(SecurityServiceBean.class.getName());

	@Resource
	private SessionContext ctx;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
	
	public SecurityServiceBean() {
	}

	public SessionContext getSessionContext() {
		return ctx;
	}

	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
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

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void changeOwner(String key, String newowner) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "changing owner to subject [" + newowner + "] on key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkOwnership(key, subjects);
			if ( registry.isLocked(key) ) {
				throw new SecurityServiceException("key [" + key + "] is locked and cannot be modified.");
			}
			OrtolangObjectIdentifier keyid = registry.lookup(key);
			OrtolangObjectIdentifier identifier = registry.lookup(newowner);
			if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
				throw new SecurityServiceException("new owner must be an object managed by " + MembershipService.SERVICE_NAME);
			}
			authorisation.updatePolicyOwner(key, newowner);
			notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "change-owner"), "owner=" + newowner);
			logger.log(Level.INFO, "owner changed to [" + newowner + "] for key [" + key + "]");
		} catch (MembershipServiceException | RegistryServiceException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new SecurityServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getOwner(String key) throws SecurityServiceException, AccessDeniedException {
		logger.log(Level.INFO, "getting owner for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			OrtolangObjectIdentifier keyid = registry.lookup(key);
			notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "get-owner"), "");
			return authorisation.getPolicyOwner(key);
		} catch (MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | NotificationServiceException | RegistryServiceException e) {
			throw new SecurityServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Map<String, List<String>> listRules(String key) throws SecurityServiceException, AccessDeniedException {
		logger.log(Level.INFO, "listing rules for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			OrtolangObjectIdentifier keyid = registry.lookup(key);
			notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "list-rules"), "");
			return authorisation.getPolicyRules(key);
		} catch (MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | RegistryServiceException | NotificationServiceException e) {
			throw new SecurityServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setRules(String key, Map<String, List<String>> rules) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "setting rules for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkOwnership(key, subjects);
			if ( registry.isLocked(key) ) {
				throw new SecurityServiceException("key [" + key + "] is locked and cannot be modified.");
			}
			
			OrtolangObjectIdentifier keyid = registry.lookup(key);
			for ( String subject : rules.keySet() ) {
				OrtolangObjectIdentifier identifier = registry.lookup(subject);
				if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
					throw new SecurityServiceException("rule subject must be an object managed by " + MembershipService.SERVICE_NAME);
				}
			}
			authorisation.setPolicyRules(key, rules);
			notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "set-rules"), "");
		} catch (MembershipServiceException | KeyNotFoundException | RegistryServiceException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new SecurityServiceException(e);
		}
		throw new SecurityServiceException("not implemented");
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setRule(String key, String subject, List<String> permissions) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "setting rule for key [" + key + "] and subject [" + subject + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkOwnership(key, subjects);
			if ( registry.isLocked(key) ) {
				throw new SecurityServiceException("key [" + key + "] is locked and cannot be modified.");
			}
			
			OrtolangObjectIdentifier keyid = registry.lookup(key);
			OrtolangObjectIdentifier identifier = registry.lookup(subject);
			if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
				throw new SecurityServiceException("rule subject must be an object managed by " + MembershipService.SERVICE_NAME);
			}
			Map<String, List<String>> rules = authorisation.getPolicyRules(key);
			if ( permissions == null || permissions.size() == 0 ) {
				rules.remove(subject);
			} else {
				rules.put(subject, permissions);
			}
			authorisation.setPolicyRules(key, rules);
			notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "set-rule"), "subject=" + subject + ", permissions=" + permissions);
		} catch (MembershipServiceException | KeyNotFoundException | RegistryServiceException | AuthorisationServiceException | NotificationServiceException e) {
			ctx.setRollbackOnly();
			throw new SecurityServiceException(e);
		}
		throw new SecurityServiceException("not implemented");
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> listAvailablePermissions(String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing availables permissions for key [" + key + "]");
//		try {
//			String caller = membership.getProfileKeyForConnectedIdentifier();
//			List<String> subjects = membership.getConnectedIdentifierSubjects();
//			authorisation.checkPermission(key, subjects, "read");
//			
//			OrtolangObjectIdentifier keyid = registry.lookup(key);
			//TODO find a way to touch concrete service from service name 
			throw new SecurityServiceException("not implemented");
			
			//notification.throwEvent(key, caller, keyid.getType(), OrtolangEvent.buildEventType(keyid.getService(), keyid.getType(), "list-permissions"), "");
//		} catch (MembershipServiceException | KeyNotFoundException | RegistryServiceException | AuthorisationServiceException e) {
//			ctx.setRollbackOnly();
//			throw new SecurityServiceException(e);
//		}
	}

	@Override
	public String getServiceName() {
		return SecurityService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return new String[] {};
	}
	
	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("this service does not manage any object");
	}

}
