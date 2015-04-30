package fr.ortolang.diffusion.browser;

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Local(BrowserService.class)
@Stateless(name = BrowserService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class BrowserServiceBean implements BrowserService {

	private static final Logger LOGGER = Logger.getLogger(BrowserServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { };
	private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
	
	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexingService indexing;

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
		LOGGER.log(Level.FINE, "looking up identifier for key [" + key + "]");
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
	public List<String> list(int offset, int limit, String service, String type, OrtolangObjectState.Status status, boolean itemsOnly) throws BrowserServiceException {
		LOGGER.log(Level.FINE, "listing keys");
		try {
			LOGGER.log(Level.FINE, "auth user: " + membership.getProfileKeyForConnectedIdentifier());
			return registry.list(offset, limit, OrtolangObjectIdentifier.buildJPQLFilterPattern(service, type), status, itemsOnly);
		} catch (RegistryServiceException e) {
			throw new BrowserServiceException("error during listing keys", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long count(String service, String type, OrtolangObjectState.Status status, boolean itemsOnly) throws BrowserServiceException {
		LOGGER.log(Level.FINE, "counting keys");
		try {
			return registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(service, type), status, itemsOnly);
		} catch (RegistryServiceException e) {
			throw new BrowserServiceException("error during couting keys", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "listing properties for key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			List<OrtolangObjectProperty> properties = registry.getProperties(key); 
			return properties;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new BrowserServiceException("error during listing entries", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "getting property with name [" + name + "] for key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			String value = registry.getProperty(key, name);
			OrtolangObjectProperty property = new OrtolangObjectProperty(name, value);
			return property;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new BrowserServiceException("error during getting property", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setProperty(String key, String name, String value) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException {
		LOGGER.log(Level.INFO, "setting property with name [" + name + "] for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			if ( value.startsWith(OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX ) ) {
				authorisation.checkSuperUser(caller);
			}
			authorisation.checkPermission(key, subjects, "update");
			registry.setProperty(key, name, value);
			notification.throwEvent(key, caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(BrowserService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "set-property"), "name=" + name + ", value=" + value);
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException e) {
			throw new BrowserServiceException("error during getting property", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "getting state for key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			boolean hidden = registry.isHidden(key);
			String lock = registry.getLock(key);
			String status = registry.getPublicationStatus(key);
			long lastModification = registry.getLastModificationDate(key);
			OrtolangObjectState state = new OrtolangObjectState(hidden, lock, status, lastModification);
			return state;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new BrowserServiceException("error during getting state", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectInfos getInfos(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "getting infos for key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			String author = registry.getAuthor(key);
			long creationDate = registry.getCreationDate(key);
			long lastModificationDate = registry.getLastModificationDate(key);
			OrtolangObjectInfos infos = new OrtolangObjectInfos(author, creationDate, lastModificationDate);
			return infos;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new BrowserServiceException("error during getting infos", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectVersion getVersion(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "getting version for key [" + key + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			
			OrtolangObjectVersion version = new OrtolangObjectVersion();
			version.setAuthor(registry.getAuthor(key));
			version.setDate(registry.getLastModificationDate(key));
			version.setParent(registry.getParent(key));
			version.setChildren(registry.getChildren(key));
			version.setPublicationStatus(registry.getPublicationStatus(key));
			version.setKey(key);
			
			return version;
		} catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			throw new BrowserServiceException("error during getting version", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectVersion> getHistory(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "getting history for key [" + key + "]");
		
		List<OrtolangObjectVersion> versions = new ArrayList<OrtolangObjectVersion> ();
		String parent = key;
		while ( parent != null ) {
			OrtolangObjectVersion version = getVersion(parent);
			versions.add(version);
			parent = version.getParent();
		}
		return versions;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void index(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "indexing key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkSuperUser(caller);
			indexing.index(key);
		} catch (IndexingServiceException | AuthorisationServiceException e) {
			throw new BrowserServiceException("unable to index key " + key, e);
		}
	}

	@Override
	public String getServiceName() {
		return BrowserService.SERVICE_NAME;
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
	public OrtolangObject findObject(String key) throws OrtolangException, AccessDeniedException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "trying to find object for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangService service = OrtolangServiceLocator.findService(identifier.getService());
			return service.findObject(key);
		} catch (RegistryServiceException e) {
			throw new OrtolangException("unable to find object for key [" + key + "]", e);
		}
	}

    // @TODO implement getSize
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
    }

}
