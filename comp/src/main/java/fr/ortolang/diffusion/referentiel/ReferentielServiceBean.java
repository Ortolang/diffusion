package fr.ortolang.diffusion.referentiel;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
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
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.referentiel.entity.ReferentielEntity;
import fr.ortolang.diffusion.referentiel.entity.ReferentielType;
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

@Local(ReferentielService.class)
@Stateless(name = ReferentielService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ReferentielServiceBean implements ReferentielService {

	private static final Logger LOGGER = Logger.getLogger(ReferentielServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { ReferentielEntity.OBJECT_TYPE };
	private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ ReferentielEntity.OBJECT_TYPE, "read,update,delete" }};
	public static final String PREFIX_KEY = "person";
	
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
	public String[] getObjectPermissionsList(String type)
			throws OrtolangException {
		for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
			if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
				return OBJECT_PERMISSIONS_LIST[i][1].split(",");
			}
		}
		throw new OrtolangException("Unable to find object permissions list for object type : " + type);
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObject findObject(String key) throws OrtolangException,
			KeyNotFoundException, AccessDeniedException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(ReferentielService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(ReferentielEntity.OBJECT_TYPE)) {
				return readReferentielEntity(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (ReferentielServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}


	@Override
	public OrtolangObjectSize getSize(String key) throws OrtolangException,
			KeyNotFoundException, AccessDeniedException {
		return null;
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ReferentielEntity> listReferentielEntities() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "Listing all ReferentielEntitys");
		try {
			TypedQuery<ReferentielEntity> query = em.createNamedQuery("findAllReferentielEntities", ReferentielEntity.class);
			List<ReferentielEntity> refEntitys = query.getResultList();
			List<ReferentielEntity> rrefEntitys = new ArrayList<ReferentielEntity>();
			for (ReferentielEntity refEntity : refEntitys) {
				try {
					String ikey = registry.lookup(refEntity.getObjectIdentifier());
					refEntity.setKey(ikey);
					rrefEntitys.add(refEntity);
				} catch (IdentifierNotRegisteredException e) {
					LOGGER.log(Level.FINE, "unregistered form found in storage for id: " + refEntity.getId());
				}
			}
			return rrefEntitys;
		} catch (RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while listing ReferentielEntitys", e);
			throw new ReferentielServiceException("unable to list ReferentielEntitys", e);
		}
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createReferentielEntity(String name, ReferentielType type, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
		LOGGER.log(Level.FINE, "creating ReferentielEntity for identifier name [" + name + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			
			String key = PREFIX_KEY+":"+name;
			ReferentielEntity refEntity = new ReferentielEntity();
			refEntity.setId(UUID.randomUUID().toString());
			refEntity.setKey(key);
			refEntity.setName(name);
			refEntity.setType(type);
			refEntity.setContent(content);
			em.persist(refEntity);

			registry.register(key, refEntity.getObjectIdentifier(), caller);
			registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
			indexing.index(key);
			
			authorisation.createPolicy(key, caller);
			
			notification.throwEvent(key, caller, ReferentielEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, ReferentielEntity.OBJECT_TYPE, "create"));
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("unable to create ReferentielEntity with name [" + name + "]", e);
		}
	}


	@Override
	public ReferentielEntity readReferentielEntity(String name) throws ReferentielServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "reading form for name [" + name + "]");
		try {
			String key = PREFIX_KEY+":"+name;
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ReferentielEntity.OBJECT_TYPE);
			ReferentielEntity refEntity = em.find(ReferentielEntity.class, identifier.getId());
			if (refEntity == null) {
				throw new ReferentielServiceException("unable to find a ReferentielEntity for id " + identifier.getId());
			}
			refEntity.setKey(key);

			return refEntity;
		} catch (RegistryServiceException e) {
			throw new ReferentielServiceException("unable to read the ReferentielEntity with name [" + name + "]", e);
		}
	}


	@Override
	public void updateReferentielEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating form for name [" + name + "]");
		try {
			String key = PREFIX_KEY+":"+name;
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ReferentielEntity.OBJECT_TYPE);
			ReferentielEntity refEntity = em.find(ReferentielEntity.class, identifier.getId());
			if (refEntity == null) {
				throw new ReferentielServiceException("unable to find a form for id " + identifier.getId());
			}
			refEntity.setKey(key);
			refEntity.setName(name);
			refEntity.setContent(content);
			em.merge(refEntity);

			registry.update(key);

			notification.throwEvent(key, caller, ReferentielEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, ReferentielEntity.OBJECT_TYPE, "update"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("error while trying to update the ReferentielEntity with name [" + name + "]");
		}
	}


	@Override
	public void deleteReferentielEntity(String name) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting form for name [" + name + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			String key = PREFIX_KEY+":"+name;
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, ReferentielEntity.OBJECT_TYPE);
			registry.delete(key);
			notification.throwEvent(key, caller, ReferentielEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, ReferentielEntity.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("unable to delete ReferentielEntity with name [" + name + "]", e);
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
	public IndexablePlainTextContent getIndexablePlainTextContent(String key)
			throws OrtolangException {
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

			if (identifier.getType().equals(ReferentielEntity.OBJECT_TYPE)) {
				ReferentielEntity referentielEntity = em.find(ReferentielEntity.class, identifier.getId());
				if (referentielEntity == null) {
					throw new OrtolangException("unable to load ReferentielEntity with id [" + identifier.getId() + "] from storage");
				}
				
				content.put("ortolang-referentiel-json", new ByteArrayInputStream(referentielEntity.getContent().getBytes()));
			}

			return content;
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}
}
