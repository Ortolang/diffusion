package fr.ortolang.diffusion.referential;

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
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
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

@Local(ReferentialService.class)
@Stateless(name = ReferentialService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ReferentialServiceBean implements ReferentialService {

    private static final Logger LOGGER = Logger.getLogger(ReferentialServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { ReferentialEntity.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { ReferentialEntity.OBJECT_TYPE, "read,update,delete" } };

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

    public ReferentialServiceBean() {
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

            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            if (identifier.getType().equals(ReferentialEntity.OBJECT_TYPE)) {
        		return readEntity(key);
            }
            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (ReferentialServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
    }

	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ReferentialEntity> listEntities(ReferentialEntityType type) throws ReferentialServiceException {
		LOGGER.log(Level.FINE, "Listing all entities");
    	try {
    		TypedQuery<ReferentialEntity> query = em.createNamedQuery("findAllEntitiesWithType", ReferentialEntity.class).setParameter("type", type);
    		
    		List<ReferentialEntity> refEntitys = query.getResultList();
    		List<ReferentialEntity> rrefEntitys = new ArrayList<ReferentialEntity>();
    		for (ReferentialEntity refEntity : refEntitys) {
    			try {
    				String ikey = registry.lookup(refEntity.getObjectIdentifier());
    				refEntity.setKey(ikey);
    				rrefEntitys.add(refEntity);
    			} catch (IdentifierNotRegisteredException e) {
    				LOGGER.log(Level.FINE, "unregistered entity found in storage for id: " + refEntity.getId());
    			}
    		}
    		return rrefEntitys;
    	} catch (RegistryServiceException e) {
    		LOGGER.log(Level.SEVERE, "unexpected error occured while listing eEntities", e);
    		throw new ReferentialServiceException("unable to list entities", e);
    	}
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createEntity(String name, ReferentialEntityType type, String content)
			throws ReferentialServiceException, KeyAlreadyExistsException,
			AccessDeniedException {
		LOGGER.log(Level.FINE, "creating ReferentielEntity for identifier name [" + name + "]");
    	try {
    		String caller = membership.getProfileKeyForConnectedIdentifier();
    		List<String> subjects = membership.getConnectedIdentifierSubjects();
    		authorisation.checkAuthentified(subjects);

    		String key = SERVICE_NAME + ":" + name;
    		
    		ReferentialEntity refEntity = new ReferentialEntity();
    		refEntity.setId(UUID.randomUUID().toString());
    		refEntity.setKey(key);
    		refEntity.setType(type);
    		refEntity.setContent(content);

    		registry.register(key, refEntity.getObjectIdentifier(), caller);
    		registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());

    		em.persist(refEntity);
    		indexing.index(key);
    		authorisation.createPolicy(key, caller);

    		notification.throwEvent(key, caller, ReferentialEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, "create"));
    	} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
    			| KeyLockedException | IndexingServiceException e) {
    		ctx.setRollbackOnly();
    		throw new ReferentialServiceException("unable to create ReferentielEntity with name [" + name + "]", e);
    	}
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ReferentialEntity readEntity(String name)
			throws ReferentialServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "reading ReferentialEntity for name [" + name + "]");
    	try {

    		String key = SERVICE_NAME + ":" + name;
    		OrtolangObjectIdentifier identifier = registry.lookup(key);
    		checkObjectType(identifier, ReferentialEntity.OBJECT_TYPE);
    		ReferentialEntity refEntity = em.find(ReferentialEntity.class, identifier.getId());
    		if (refEntity == null) {
    			throw new ReferentialServiceException("unable to find a ReferentialEntity for id " + identifier.getId());
    		}
    		refEntity.setKey(key);

    		return refEntity;
    	} catch (RegistryServiceException e) {
    		throw new ReferentialServiceException("unable to read the ReferentialEntity with name [" + name + "]", e);
    	}
	}

	@Override
	public void updateEntity(String name, ReferentialEntityType type, String content) 
			throws ReferentialServiceException, KeyNotFoundException,
			AccessDeniedException {
		LOGGER.log(Level.FINE, "updating ReferentialEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, ReferentialEntity.OBJECT_TYPE);
            ReferentialEntity refEntity = em.find(ReferentialEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentialServiceException("unable to find a ReferentialEntity for id " + identifier.getId());
            }
            refEntity.setKey(key);
            refEntity.setType(type);
            refEntity.setContent(content);

            registry.update(key);
            em.merge(refEntity);
            indexing.index(key);

            notification.throwEvent(key, caller, ReferentialEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentialServiceException("error while trying to update the ReferentialEntity with name [" + name + "]");
        }
	}

    
    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws ReferentialServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new ReferentialServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new ReferentialServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }
    

	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public IndexablePlainTextContent getIndexablePlainTextContent(String key)
			throws OrtolangException, NotIndexableContentException {
		throw new NotIndexableContentException();
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public IndexableJsonContent getIndexableJsonContent(String key)
			throws OrtolangException, NotIndexableContentException {
		 try {
	            OrtolangObjectIdentifier identifier = registry.lookup(key);
	            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
	                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
	            }
	            IndexableJsonContent content = new IndexableJsonContent();

	            if (identifier.getType().equals(ReferentialEntity.OBJECT_TYPE)) {
	            	ReferentialEntity referentielEntity = em.find(ReferentialEntity.class, identifier.getId());
	                if (referentielEntity == null) {
	                    throw new OrtolangException("unable to load ReferentialEntity with id [" + identifier.getId() + "] from storage");
	                }
	                String json = referentielEntity.getContent();
	    			List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(json);
	    			for(String ortolangKey : ortolangKeys) {
	    				json = OrtolangKeyExtractor.replaceOrtolangKey(ortolangKey, json);
	    			}
	                content.put("ortolang-referential-json", json);
	            }
	            return content;
	        } catch (RegistryServiceException | KeyNotFoundException e) {
	            throw new OrtolangException("unable to find an object for key " + key);
	        }
	}
}
