package fr.ortolang.diffusion.security.authorisation;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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
@PermitAll
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
	public void checkAuthentified(List<String> subjects) throws AuthorisationServiceException, AccessDeniedException {
		logger.log(Level.FINE, "checking authentification state");
		if (subjects.contains(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY)) {
			return;
		}
		throw new AccessDeniedException("subjects does not contains " + MembershipService.ALL_AUTHENTIFIED_GROUP_KEY + " group key");
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
