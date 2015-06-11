package fr.ortolang.diffusion.referentiel;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import fr.ortolang.diffusion.referentiel.entity.Organization;
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
import fr.ortolang.diffusion.store.triple.IndexableSemanticContent;
import fr.ortolang.diffusion.store.triple.Triple;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;
import fr.ortolang.diffusion.store.triple.URIHelper;

@Local(ReferentielService.class)
@Stateless(name = ReferentielService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ReferentielServiceBean implements ReferentielService {

	private static final Logger LOGGER = Logger.getLogger(ReferentielServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { Organization.OBJECT_TYPE };
	private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Organization.OBJECT_TYPE, "read,update,delete" }};

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

			if (identifier.getType().equals(Organization.OBJECT_TYPE)) {
				return readOrganization(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (ReferentielServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}


	@Override
	public OrtolangObjectSize getSize(String key) throws OrtolangException,
			KeyNotFoundException, AccessDeniedException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Organization> listOrganizations() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "Listing all organizations");
		try {
			TypedQuery<Organization> query = em.createNamedQuery("findAllOrganizations", Organization.class);
			List<Organization> orgs = query.getResultList();
			List<Organization> rorgs = new ArrayList<Organization>();
			for (Organization org : orgs) {
				try {
					String ikey = registry.lookup(org.getObjectIdentifier());
					org.setKey(ikey);
					rorgs.add(org);
				} catch (IdentifierNotRegisteredException e) {
					LOGGER.log(Level.FINE, "unregistered form found in storage for id: " + org.getId());
				}
			}
			return rorgs;
		} catch (RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while listing organizations", e);
			throw new ReferentielServiceException("unable to list organizations", e);
		}
	}


	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createOrganization(String identifier, String name, String fullname, String acronym, String city, String country, String homepage, String img) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException {
		LOGGER.log(Level.FINE, "creating organization for identifier [" + identifier + "] and name [" + name + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);
			
			String key = getOrganizationKeyForIdentifier(identifier);

			Organization org = new Organization();
			org.setId(identifier);
			org.setName(name);
			org.setFullname(fullname);
			org.setAcronym(acronym);
			org.setCity(city);
			org.setCountry(country);
			org.setHomepage(homepage);
			org.setImg(img);
			em.persist(org);

			registry.register(key, org.getObjectIdentifier(), caller);
			registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());
			indexing.index(key);
			
			authorisation.createPolicy(key, caller);
			
			notification.throwEvent(key, caller, Organization.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, Organization.OBJECT_TYPE, "create"));
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | IndexingServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("unable to create organization with identifier [" + identifier + "]", e);
		}
	}


	@Override
	public Organization readOrganization(String key) throws ReferentielServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "reading form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Organization.OBJECT_TYPE);
			Organization org = em.find(Organization.class, identifier.getId());
			if (org == null) {
				throw new ReferentielServiceException("unable to find a organization for id " + identifier.getId());
			}
			org.setKey(key);

			notification.throwEvent(key, caller, Organization.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, Organization.OBJECT_TYPE, "read"));
			return org;
		} catch (RegistryServiceException | NotificationServiceException e) {
			throw new ReferentielServiceException("unable to read the organization with key [" + key + "]", e);
		}
	}


	@Override
	public void updateOrganzation(String key, String name, String fullname, String acronym, String city, String country, String homepage, String img) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Organization.OBJECT_TYPE);
			Organization org = em.find(Organization.class, identifier.getId());
			if (org == null) {
				throw new ReferentielServiceException("unable to find a form for id " + identifier.getId());
			}
			org.setName(name);
			org.setFullname(fullname);
			org.setAcronym(acronym);
			org.setCity(city);
			org.setCountry(country);
			org.setHomepage(homepage);
			org.setImg(img);
			em.merge(org);

			registry.update(key);

			notification.throwEvent(key, caller, Organization.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, Organization.OBJECT_TYPE, "update"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("error while trying to update the organization with key [" + key + "]");
		}
	}


	@Override
	public void deleteOrganization(String key) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Organization.OBJECT_TYPE);
			registry.delete(key);
			notification.throwEvent(key, caller, Organization.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentielService.SERVICE_NAME, Organization.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new ReferentielServiceException("unable to delete organization with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	@PermitAll
	public String getOrganizationKeyForIdentifier(String identifier) {
		return identifier;
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

			if (identifier.getType().equals(Organization.OBJECT_TYPE)) {
				Organization org = em.find(Organization.class, identifier.getId());
				if (org != null) {
					content.addContentPart(org.getFullname());
					content.addContentPart(org.getHomepage());
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException e) {
			throw new OrtolangException("unable to get indexable plain text content for key " + key, e);
		}
	}


	@Override
	public IndexableSemanticContent getIndexableSemanticContent(String key)
			throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(getServiceName())) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			IndexableSemanticContent content = new IndexableSemanticContent();
			
			if (identifier.getType().equals(Organization.OBJECT_TYPE)) {
				Organization org = em.find(Organization.class, identifier.getId());
				if (org != null) {
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://xmlns.com/foaf/0.1/Organization"));
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/name", org.getName()));
					content.addTriple(new Triple(URIHelper.fromKey(key), "http://xmlns.com/foaf/0.1/homepage", org.getHomepage()));
				}
			}

			return content;
		} catch (KeyNotFoundException | RegistryServiceException | TripleStoreServiceException e) {
			throw new OrtolangException("unable to get indexable semantic content for key " + key, e);
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

			if (identifier.getType().equals(Organization.OBJECT_TYPE)) {
				Organization organization = em.find(Organization.class, identifier.getId());
				if (organization == null) {
					throw new OrtolangException("unable to load organization with id [" + identifier.getId() + "] from storage");
				}
				
				ObjectMapper mapper = new ObjectMapper();
				byte[] orgBytes = mapper.writeValueAsBytes(organization);
				content.put("ortolang-referentiel-json", new ByteArrayInputStream(orgBytes));
			}

			return content;
		} catch (RegistryServiceException | KeyNotFoundException | JsonProcessingException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}
}
