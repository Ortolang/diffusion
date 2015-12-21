package fr.ortolang.diffusion.referentiel;

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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.referentiel.entity.LicenseEntity;
import fr.ortolang.diffusion.referentiel.entity.OrganizationEntity;
import fr.ortolang.diffusion.referentiel.entity.PersonEntity;
import fr.ortolang.diffusion.referentiel.entity.StatusOfUseEntity;
import fr.ortolang.diffusion.referentiel.entity.TermEntity;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;
import fr.ortolang.diffusion.store.json.OrtolangKeyExtractor;

@Local(ReferentielService.class)
@Stateless(name = ReferentielService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ReferentielServiceBean implements ReferentielService {

    private static final Logger LOGGER = Logger.getLogger(ReferentielServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { OrganizationEntity.OBJECT_TYPE, PersonEntity.OBJECT_TYPE, StatusOfUseEntity.OBJECT_TYPE, LicenseEntity.OBJECT_TYPE, TermEntity.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { OrganizationEntity.OBJECT_TYPE, PersonEntity.OBJECT_TYPE, StatusOfUseEntity.OBJECT_TYPE, LicenseEntity.OBJECT_TYPE, TermEntity.OBJECT_TYPE, "read,update,delete" } };

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
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    public ReferentielServiceBean() {
    }

    public RegistryService getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryService registry) {
        this.registry = registry;
    }

    public NotificationService getNotification() {
        return notification;
    }

    public void setNotification(NotificationService notification) {
        this.notification = notification;
    }

    public MembershipService getMembership() {
        return membership;
    }

    public void setMembership(MembershipService membership) {
        this.membership = membership;
    }

    public AuthorisationService getAuthorisation() {
        return authorisation;
    }

    public void setAuthorisation(AuthorisationService authorisation) {
        this.authorisation = authorisation;
    }

    public IndexingService getIndexing() {
        return indexing;
    }

    public void setIndexing(IndexingService indexing) {
        this.indexing = indexing;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public SessionContext getCtx() {
        return ctx;
    }

    public void setCtx(SessionContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
            if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
                return OBJECT_PERMISSIONS_LIST[i][1].split(",");
            }
        }
        throw new OrtolangException("Unable to find object permissions list for object type : " + type);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObject findObject(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(ReferentielService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            if (identifier.getType().equals(OrganizationEntity.OBJECT_TYPE)) {
        		return readOrganizationEntity(key);
            }
            if (identifier.getType().equals(PersonEntity.OBJECT_TYPE)) {
        		return readPersonEntity(key);
            }
            if (identifier.getType().equals(StatusOfUseEntity.OBJECT_TYPE)) {
        		return readStatusOfUseEntity(key);
            }
            if (identifier.getType().equals(LicenseEntity.OBJECT_TYPE)) {
        		return readLicenseEntity(key);
            }
            if (identifier.getType().equals(TermEntity.OBJECT_TYPE)) {
        		return readTermEntity(key);
            }
            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (ReferentielServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrganizationEntity> listOrganizationEntities() throws ReferentielServiceException {
    	LOGGER.log(Level.INFO, "Listing all OrganizationEntities");
    	try {
    		TypedQuery<OrganizationEntity> query = em.createNamedQuery("findAllOrganizationEntities", OrganizationEntity.class);
    		List<OrganizationEntity> refEntitys = query.getResultList();
    		List<OrganizationEntity> rrefEntitys = new ArrayList<OrganizationEntity>();
    		for (OrganizationEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered OrganizationEntity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing OrganizationEntities", e);
    		throw new ReferentielServiceException("unable to list OrganizationEntities", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createOrganizationEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "creating ReferentielEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		String key = SERVICE_NAME + ":" + name;
    		OrganizationEntity refEntity = new OrganizationEntity();
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setContent(content);
    		em.persist(refEntity);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
    		indexing.index(key);

    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, OrganizationEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, OrganizationEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentielServiceException("unable to create ReferentielEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrganizationEntity readOrganizationEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
    	LOGGER.log(Level.FINE, "reading OrganizationEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, OrganizationEntity.OBJECT_TYPE);
    		OrganizationEntity refEntity = em.find(OrganizationEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentielServiceException("unable to find a OrganizationEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentielServiceException("unable to read the OrganizationEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateOrganizationEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "updating OrganizationEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
    		String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, OrganizationEntity.OBJECT_TYPE);
            OrganizationEntity refEntity = em.find(OrganizationEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentielServiceException("unable to find a OrganizationEntity for id " + identifier.getId());
            }
            refEntity.setKey(key);
            refEntity.setContent(content);
            em.merge(refEntity);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, OrganizationEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, OrganizationEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentielServiceException("error while trying to update the OrganizationEntity with name [" + name + "]");
        }
    }

	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<PersonEntity> listPersonEntities() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "Listing all PersonEntity");
    	try {
    		TypedQuery<PersonEntity> query = em.createNamedQuery("findAllPersonEntities", PersonEntity.class);
    		List<PersonEntity> refEntitys = query.getResultList();
    		List<PersonEntity> rrefEntitys = new ArrayList<PersonEntity>();
    		for (PersonEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered PersonEntity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing PersonEntities", e);
    		throw new ReferentielServiceException("unable to list PersonEntities", e);
    	}
	}
 

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createPersonEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "creating PersonEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		OrganizationEntity orgEntity = null;
    		String organization = extractField(content, "organization");
    		
    		if(organization!=null) {
	    		List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(organization);
	    		if(ortolangKeys.size()==1) {
	    			organization = ortolangKeys.get(0);

	    			OrtolangObjectIdentifier identifier = registry.lookup(organization);
	        		checkObjectType(identifier, OrganizationEntity.OBJECT_TYPE);
	        		orgEntity = em.find(OrganizationEntity.class, identifier.getId());
	        		if (orgEntity == null) {
	        			throw new ReferentielServiceException("unable to find a OrganizationEntity for id " + identifier.getId() + " and key " + organization);
	        		}
	        		orgEntity.setKey(organization);
	    		} else {
	    			LOGGER.log(Level.SEVERE, "unable to extract Organization in "+organization);
	    			throw new ReferentielServiceException("unable to extract a OrganizationEntity in "+organization);
	    		}
    		}
    		
    		String key = SERVICE_NAME + ":" + name;
    		PersonEntity refEntity = new PersonEntity();
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setContent(content);
    		if(orgEntity!=null) {
    			refEntity.setOrganization(orgEntity.getKey());
    		}
    		em.persist(refEntity);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
    		indexing.index(key);

    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, PersonEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, PersonEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentielServiceException("unable to create PersonEntity with name [" + name + "]", e);
    	}
    }
	
    public PersonEntity readPersonEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
    	LOGGER.log(Level.FINE, "reading PersonEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, PersonEntity.OBJECT_TYPE);
    		PersonEntity refEntity = em.find(PersonEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentielServiceException("unable to find a PersonEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentielServiceException("unable to read the PersonEntity with name [" + name + "]", e);
    	}
    }
    
    public void updatePersonEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "updating PersonEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

    		String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, PersonEntity.OBJECT_TYPE);
            PersonEntity refEntity = em.find(PersonEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentielServiceException("unable to find a PersonEntity for id " + identifier.getId());
            }
            
            OrganizationEntity orgEntity = null;
            String organization = extractField(content, "organization");


            if(organization!=null) {
        		List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(organization);
        		if(ortolangKeys.size()==1) {
        			organization = ortolangKeys.get(0);
        			
        			OrtolangObjectIdentifier identifierOrganization = registry.lookup(organization);
            		checkObjectType(identifierOrganization, OrganizationEntity.OBJECT_TYPE);
            		orgEntity = em.find(OrganizationEntity.class, identifierOrganization.getId());
            		if (orgEntity == null) {
            			throw new ReferentielServiceException("unable to find a OrganizationEntity for id " + identifierOrganization.getId() + " and key " + organization);
            		}
            		orgEntity.setKey(organization);
        		} else {
        			LOGGER.log(Level.SEVERE, "unable to extract Organization in "+organization);
        			throw new ReferentielServiceException("unable to extract a OrganizationEntity in "+organization);
        		}
            }
    		
            refEntity.setKey(key);
            refEntity.setContent(content);
            if(orgEntity!=null) {
            	refEntity.setOrganization(orgEntity.getKey());
            }
            em.merge(refEntity);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, PersonEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, PersonEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentielServiceException("error while trying to update the PersonEntity with name [" + name + "]");
        }
    }
	
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<StatusOfUseEntity> listStatusOfUseEntities() throws ReferentielServiceException {
    	LOGGER.log(Level.INFO, "Listing all StatusOfUseEntity");
    	try {
    		TypedQuery<StatusOfUseEntity> query = em.createNamedQuery("findAllStatusOfUseEntities", StatusOfUseEntity.class);
    		List<StatusOfUseEntity> refEntitys = query.getResultList();
    		List<StatusOfUseEntity> rrefEntitys = new ArrayList<StatusOfUseEntity>();
    		for (StatusOfUseEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered StatusOfUseEntity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing StatusOfUse entities", e);
    		throw new ReferentielServiceException("unable to list of StatusOfUseEntity", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createStatusOfUseEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "creating StatusOfUseEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		String key = SERVICE_NAME + ":" + name;
    		StatusOfUseEntity refEntity = new StatusOfUseEntity();
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setContent(content);
    		em.persist(refEntity);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
    		indexing.index(key);

    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, StatusOfUseEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, StatusOfUseEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentielServiceException("unable to create StatusOfUseEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public StatusOfUseEntity readStatusOfUseEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
    	LOGGER.log(Level.FINE, "reading StatusOfUseEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, StatusOfUseEntity.OBJECT_TYPE);
    		StatusOfUseEntity refEntity = em.find(StatusOfUseEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentielServiceException("unable to find a StatusOfUseEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentielServiceException("unable to read the StatusOfUseEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateStatusOfUseEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "updating StatusOfUseEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
    		String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, StatusOfUseEntity.OBJECT_TYPE);
            StatusOfUseEntity refEntity = em.find(StatusOfUseEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentielServiceException("unable to find a StatusOfUseEntity for id " + identifier.getId());
            }
            refEntity.setKey(key);
            refEntity.setContent(content);
            em.merge(refEntity);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, StatusOfUseEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, StatusOfUseEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentielServiceException("error while trying to update the StatusOfUseEntity with name [" + name + "]");
        }
    }

	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<LicenseEntity> listLicenseEntities() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "Listing all LicenseEntity");
    	try {
    		TypedQuery<LicenseEntity> query = em.createNamedQuery("findAllLicenseEntities", LicenseEntity.class);
    		List<LicenseEntity> refEntitys = query.getResultList();
    		List<LicenseEntity> rrefEntitys = new ArrayList<LicenseEntity>();
    		for (LicenseEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered LicenseEntity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing LicenseEntities", e);
    		throw new ReferentielServiceException("unable to list LicenseEntities", e);
    	}
	}
 
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createLicenseEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "creating LicenseEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		StatusOfUseEntity statusOfUseEntity = null;
    		String statusOfUse = extractField(content, "status");
//    		statusOfUse = statusOfUse.replaceAll(JsonStoreServiceBean.PREFIX_REF, "");
//    		if(statusOfUse!=null) {

    		List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(statusOfUse);
    		if(ortolangKeys.size()==1) {
    			statusOfUse = ortolangKeys.get(0);
    			
    			OrtolangObjectIdentifier identifier = registry.lookup(statusOfUse);
        		checkObjectType(identifier, StatusOfUseEntity.OBJECT_TYPE);
        		statusOfUseEntity = em.find(StatusOfUseEntity.class, identifier.getId());
        		if (statusOfUseEntity == null) {
        			throw new ReferentielServiceException("unable to find a StatusOfUseEntity for id " + identifier.getId() + " and key " + statusOfUse);
        		}
        		statusOfUseEntity.setKey(statusOfUse);
    		} else {
    			LOGGER.log(Level.SEVERE, "unable to extract StatusOfUse in "+statusOfUse);
    			throw new ReferentielServiceException("unable to extract a StatusOfUse in "+statusOfUse);
    		}
    		
    		LicenseEntity refEntity = new LicenseEntity();
    		String key = SERVICE_NAME + ":" + name;
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setContent(content);
    		if(statusOfUseEntity!=null) {
    			refEntity.setStatusOfUse(statusOfUseEntity.getKey());
    		}
    		em.persist(refEntity);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
    		indexing.index(key);

    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, LicenseEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, LicenseEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentielServiceException("unable to create LicenseEntity with name [" + name + "]", e);
    	}
    }
	
    public LicenseEntity readLicenseEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
    	LOGGER.log(Level.FINE, "reading LicenseEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, LicenseEntity.OBJECT_TYPE);
    		LicenseEntity refEntity = em.find(LicenseEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentielServiceException("unable to find a LicenseEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentielServiceException("unable to read the LicenseEntity with name [" + name + "]", e);
    	}
    }
    
    public void updateLicenseEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "updating LicenseEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
    		String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, LicenseEntity.OBJECT_TYPE);
            LicenseEntity refEntity = em.find(LicenseEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentielServiceException("unable to find a LicenseEntity for id " + identifier.getId());
            }
            
            StatusOfUseEntity statusOfUseEntity = null;
    		String statusOfUse = extractField(content, "status");
//    		statusOfUse = statusOfUse.replaceAll(JsonStoreServiceBean.PREFIX_REF, "");
//    		if(statusOfUse!=null) {

    		List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(statusOfUse);
    		if(ortolangKeys.size()==1) {
    			statusOfUse = ortolangKeys.get(0);
    			
    			OrtolangObjectIdentifier identifierStatusOfUse = registry.lookup(statusOfUse);
        		checkObjectType(identifierStatusOfUse, StatusOfUseEntity.OBJECT_TYPE);
        		statusOfUseEntity = em.find(StatusOfUseEntity.class, identifierStatusOfUse.getId());
        		if (statusOfUseEntity == null) {
        			throw new ReferentielServiceException("unable to find a StatusOfUseEntity for id " + identifierStatusOfUse.getId() + " and key " + statusOfUse);
        		}
        		statusOfUseEntity.setKey(statusOfUse);
    		} else {
    			throw new ReferentielServiceException("unable to extract status of use ("+statusOfUse+") in license named " + name);
    		}
    		
            refEntity.setKey(key);
            refEntity.setContent(content);
            if(statusOfUseEntity!=null) {
            	refEntity.setStatusOfUse(statusOfUseEntity.getKey());
            }
            em.merge(refEntity);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, LicenseEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, LicenseEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentielServiceException("error while trying to update the LicenseEntity with name [" + name + "]");
        }
    }
	

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<TermEntity> listTermEntities() throws ReferentielServiceException {
    	LOGGER.log(Level.INFO, "Listing all TermEntities");
    	try {
    		TypedQuery<TermEntity> query = em.createNamedQuery("findAllTermEntities", TermEntity.class);
    		List<TermEntity> refEntitys = query.getResultList();
    		List<TermEntity> rrefEntitys = new ArrayList<TermEntity>();
    		for (TermEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered TermEntity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing TermEntities", e);
    		throw new ReferentielServiceException("unable to list TermEntities", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createTermEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "creating TermEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		String key = SERVICE_NAME + ":" + name;
    		TermEntity refEntity = new TermEntity();
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setContent(content);
    		em.persist(refEntity);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
    		indexing.index(key);

    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, TermEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, TermEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentielServiceException("unable to create ReferentielEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public TermEntity readTermEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
    	LOGGER.log(Level.FINE, "reading TermEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, TermEntity.OBJECT_TYPE);
    		TermEntity refEntity = em.find(TermEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentielServiceException("unable to find a TermEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentielServiceException("unable to read the TermEntity with name [" + name + "]", e);
    	}
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateTermEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.FINE, "updating TermEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, TermEntity.OBJECT_TYPE);
            TermEntity refEntity = em.find(TermEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentielServiceException("unable to find a TermEntity for id " + identifier.getId());
            }
            refEntity.setKey(key);
            refEntity.setContent(content);
            em.merge(refEntity);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, TermEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, TermEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentielServiceException("error while trying to update the TermEntity with name [" + name + "]");
        }
    }

    
    
    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws ReferentielServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new ReferentielServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new ReferentielServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }

    @Override
    public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(ReferentielService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            IndexablePlainTextContent content = new IndexablePlainTextContent();

            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to get indexable plain text content for key " + key, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(ReferentielService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexableJsonContent content = new IndexableJsonContent();

//            if (identifier.getType().equals(ReferentielEntity.OBJECT_TYPE)) {
//                ReferentielEntity referentielEntity = em.find(ReferentielEntity.class, identifier.getId());
//                if (referentielEntity == null) {
//                    throw new OrtolangException("unable to load ReferentielEntity with id [" + identifier.getId() + "] from storage");
//                }
//
//                content.put("ortolang-referentiel-json", new ByteArrayInputStream(referentielEntity.getContent().getBytes()));
//            }
            
            if (identifier.getType().equals(OrganizationEntity.OBJECT_TYPE)) {
            	OrganizationEntity organizationEntity = em.find(OrganizationEntity.class, identifier.getId());
                if (organizationEntity == null) {
                    throw new OrtolangException("unable to load OrganizationEntity with id [" + identifier.getId() + "] from storage");
                }

                content.put("ortolang-referentiel-json", new ByteArrayInputStream(organizationEntity.getContent().getBytes()));
            }

            if (identifier.getType().equals(StatusOfUseEntity.OBJECT_TYPE)) {
            	StatusOfUseEntity statusOfUseEntity = em.find(StatusOfUseEntity.class, identifier.getId());
                if (statusOfUseEntity == null) {
                    throw new OrtolangException("unable to load StatusOfUseEntity with id [" + identifier.getId() + "] from storage");
                }

                content.put("ortolang-referentiel-json", new ByteArrayInputStream(statusOfUseEntity.getContent().getBytes()));
            }

            if (identifier.getType().equals(TermEntity.OBJECT_TYPE)) {
            	TermEntity termEntity = em.find(TermEntity.class, identifier.getId());
                if (termEntity == null) {
                    throw new OrtolangException("unable to load TermEntity with id [" + identifier.getId() + "] from storage");
                }

                content.put("ortolang-referentiel-json", new ByteArrayInputStream(termEntity.getContent().getBytes()));
            }

            if (identifier.getType().equals(PersonEntity.OBJECT_TYPE)) {
            	PersonEntity personEntity = em.find(PersonEntity.class, identifier.getId());
                if (personEntity == null) {
                    throw new OrtolangException("unable to load PersonEntity with id [" + identifier.getId() + "] from storage");
                }
                content.put("ortolang-referentiel-json", new ByteArrayInputStream(personEntity.getContent().getBytes()));
            }

            if (identifier.getType().equals(LicenseEntity.OBJECT_TYPE)) {
            	LicenseEntity licenseEntity = em.find(LicenseEntity.class, identifier.getId());
                if (licenseEntity == null) {
                    throw new OrtolangException("unable to load LicenseEntity with id [" + identifier.getId() + "] from storage");
                }
                content.put("ortolang-referentiel-json", new ByteArrayInputStream(licenseEntity.getContent().getBytes()));
            }

            return content;
        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }


	private String extractField(String jsonContent, String fieldName) {
		String fieldValue = null;
		StringReader reader = new StringReader(jsonContent);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			JsonObject jsonObj = jsonReader.readObject();
			fieldValue = jsonObj.getString(fieldName);
		} catch(NullPointerException | ClassCastException e) {
			LOGGER.log(Level.WARNING, "No property '"+fieldName+"' in json object");
		} finally {
			jsonReader.close();
			reader.close();
		}

		return fieldValue;
	}

}
