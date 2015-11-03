package fr.ortolang.diffusion.publication;

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

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Local(PublicationService.class)
@Stateless(name = PublicationService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class PublicationServiceBean implements PublicationService {

	private static final Logger LOGGER = Logger.getLogger(PublicationServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { };
	private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
	
	@EJB
	private RegistryService registry;
	@EJB
	private MembershipService membership;
	@EJB
	private CoreService core;
	@EJB
	private BinaryStoreService binaryStore;
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

	public BinaryStoreService getBinaryStore() {
		return binaryStore;
	}

	public void setBinaryStore(BinaryStoreService binaryStore) {
		this.binaryStore = binaryStore;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public CoreService getCoreService() {
		return core;
	}

	public void setCoreService(CoreService core) {
		this.core = core;
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
	public void publish(String key, Map<String, List<String>> permissions) throws PublicationServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "publishing key : " + key);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			if (!caller.equals(MembershipService.SUPERUSER_IDENTIFIER) && !subjects.contains(MembershipService.MODERATOR_GROUP_KEY)) {
				throw new AccessDeniedException("user " + caller + " is not allowed to publish, only moderators can publish content");
			}

			if (registry.getPublicationStatus(key).equals(OrtolangObjectState.Status.PUBLISHED.value())) {
				LOGGER.log(Level.WARNING, "key [" + key + "] is already published, nothing to do !!");
			} else {
				authorisation.updatePolicyOwner(key, MembershipService.SUPERUSER_IDENTIFIER);
				authorisation.setPolicyRules(key, permissions);
				registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
				registry.update(key);
				registry.lock(key, MembershipService.SUPERUSER_IDENTIFIER);
				indexing.index(key);
				notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "publish"));
			}
		} catch (KeyLockedException | AuthorisationServiceException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | MembershipServiceException | IndexingServiceException e) {
			LOGGER.log(Level.SEVERE, "error during publication of key", e);
			ctx.setRollbackOnly();
			throw new PublicationServiceException("error during publishing key : " + e);
		}
	}

	@Override
	public String getServiceName() {
		return PublicationService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		return OBJECT_PERMISSIONS_LIST;
	}

	@Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

	@Override
	public OrtolangObjectSize getSize(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

}