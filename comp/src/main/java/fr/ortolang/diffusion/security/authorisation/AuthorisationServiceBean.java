package fr.ortolang.diffusion.security.authorisation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicy;

@Local(AuthorisationService.class)
@Stateless(name = AuthorisationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class AuthorisationServiceBean implements AuthorisationService {

	private Logger logger = Logger.getLogger(AuthorisationServiceBean.class.getName());

	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;

	public AuthorisationServiceBean() {
	}

	public EntityManager getEntityManager() {
		return em;
	}

	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	public SessionContext getSessionContext() {
		return ctx;
	}

	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createPolicy(String key, String owner) throws AuthorisationServiceException {
		logger.log(Level.FINE, "creating authorisation policy for key [" + key + "]");
		AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
		if (policy != null) {
			throw new AuthorisationServiceException("a security policy already exists for key [" + key + "] in the storage");
		}
		policy = new AuthorisationPolicy();
		policy.setId(key);
		policy.setOwner(owner);
		em.persist(policy);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void clonePolicy(String key, String origin) throws AuthorisationServiceException {
		logger.log(Level.FINE, "cloning authorisation policy from origin [" + origin + "]for key [" + key + "]");
		try {
			AuthorisationPolicy opolicy = em.find(AuthorisationPolicy.class, origin);
			if (opolicy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + origin + "] in the storage");
			}
			AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
			if (policy != null) {
				throw new AuthorisationServiceException("a security policy already exists for key [" + key + "] in the storage");
			}
			policy = new AuthorisationPolicy();
			policy.setId(key);
			policy.setOwner(opolicy.getOwner());
			policy.setRules(opolicy.getRules());
			em.persist(policy);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to persist policy : " + e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void copyPolicy(String from, String to) throws AuthorisationServiceException {
		logger.log(Level.FINE, "copying authorisation policy from key [" + from + "] to key [" + to + "]");
		try {
			AuthorisationPolicy frompolicy = em.find(AuthorisationPolicy.class, from);
			if (frompolicy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + from + "] in the storage");
			}
			AuthorisationPolicy topolicy = em.find(AuthorisationPolicy.class, to);
			if (topolicy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + to + "] in the storage");
			}
			topolicy.setOwner(frompolicy.getOwner());
			topolicy.setRules(frompolicy.getRules());
			em.persist(topolicy);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to persist policy : " + e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void copyPolicy(String from, Set<String> tos) throws AuthorisationServiceException {
		logger.log(Level.FINE, "copying authorisation policy from key [" + from + "] to keys [" + Arrays.deepToString(tos.toArray()) + "]");
		try {
			AuthorisationPolicy frompolicy = em.find(AuthorisationPolicy.class, from);
			if (frompolicy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + from + "] in the storage");
			}
			for (String to : tos) {
				AuthorisationPolicy topolicy = em.find(AuthorisationPolicy.class, to);
				if (topolicy == null) {
					throw new AuthorisationServiceException("unable to find security policy for key [" + to + "] in the storage");
				}
				topolicy.setOwner(frompolicy.getOwner());
				topolicy.setRules(frompolicy.getRules());
				em.persist(topolicy);
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to persist policy : " + e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updatePolicyOwner(String key, String newowner) throws AuthorisationServiceException {
		logger.log(Level.FINE, "updating authorisation policy owner to subject [" + newowner + "] on key [" + key + "]");
		AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
		if (policy == null) {
			throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
		}
		policy.setOwner(newowner);
		em.merge(policy);
		logger.log(Level.FINE, "owner changed to [" + newowner + "] for key [" + key + "]");
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getPolicyOwner(String key) throws AuthorisationServiceException {
		logger.log(Level.FINE, "getting authorisation policy owner for key [" + key + "]");
		AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
		if (policy == null) {
			throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
		}
		return policy.getOwner();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setPolicyRules(String key, Map<String, List<String>> rules) throws AuthorisationServiceException {
		logger.log(Level.FINE, "setting authorisation policy rules for key [" + key + "]");
		try {
			AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
			if (policy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
			}
			policy.setRules(rules);
			em.merge(policy);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to persist policy : " + e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Map<String, List<String>> getPolicyRules(String key) throws AuthorisationServiceException {
		logger.log(Level.FINE, "getting authorisation policy rules for key [" + key + "]");
		try {
			AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
			if (policy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
			}
			return policy.getRules();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to read policy : " + e.getMessage(), e);
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void checkPermission(String key, List<String> subjects, String permission) throws AuthorisationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "checking permission [" + permission + "] for subjects [" + subjects + "] on key [" + key + "]");
		try {
			if (subjects.contains(MembershipService.SUPERUSER_IDENTIFIER)) {
				return;
			}
			AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
			if (policy == null) {
				throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
			}
			if (policy.hasPermission(MembershipService.UNAUTHENTIFIED_IDENTIFIER, permission)) {
				return;
			}
			for (String subject : subjects) {
				if (policy.isOwner(subject) || policy.hasPermission(subject, permission)) {
					return;
				}
			}
			throw new AccessDeniedException("permission [" + permission + "] denied for subjects [" + subjects + "] on key [" + key + "]");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to read policy : " + e.getMessage(), e);
			throw new AuthorisationServiceException("unable to persist policy", e);
		}
	}

	@Override
	public void checkOwnership(String key, List<String> subjects) throws AuthorisationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "checking ownership of subjects [" + subjects + "] on key [" + key + "]");
		if (subjects.contains(MembershipService.SUPERUSER_IDENTIFIER)) {
			return;
		}
		AuthorisationPolicy policy = em.find(AuthorisationPolicy.class, key);
		if (policy == null) {
			throw new AuthorisationServiceException("unable to find security policy for key [" + key + "] in the storage");
		}
		for (String subject : subjects) {
			if (policy.isOwner(subject)) {
				return;
			}
		}
		throw new AccessDeniedException("ownership denied for subjects [" + subjects + "] on key [" + key + "]");
	}

	@Override
	public void checkAuthentified(String identifier) throws AuthorisationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "checking authentification of identifier [" + identifier + "]");
		if (!identifier.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
			return;
		}
		throw new AccessDeniedException("identifier [" + identifier + "] is not an authenticated identifier");
	}

	@Override
	public void checkSuperUser(String identifier) throws AuthorisationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "checking super user for identifier [" + identifier + "]");
		if (identifier.equals(MembershipService.SUPERUSER_IDENTIFIER)) {
			return;
		}
		throw new AccessDeniedException("identifier [" + identifier + "] is not superuser");
	}

}
