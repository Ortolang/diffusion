package fr.ortolang.diffusion.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectTag;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.registry.entity.RegistryTag;

@Local(RegistryService.class)
@Stateless(name = RegistryService.SERVICE_NAME)
public class RegistryServiceBean implements RegistryService {

	private Logger logger = Logger.getLogger(RegistryServiceBean.class.getName());
	
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	
	public RegistryServiceBean() {
	}
	
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	public EntityManager getEntityManager() {
		return this.em;
	}

	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void create(String key, OrtolangObjectIdentifier identifier) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException {
		logger.log(Level.INFO, "creating key [" + key + "] for OOI [" + identifier + "]");
		try {
			findEntryByKey(key);
			throw new KeyAlreadyExistsException("the key [" + key + "] already exists");
		} catch (KeyNotFoundException e) {
		}
		try {
			findEntryByIdentifier(identifier);
			throw new IdentifierAlreadyRegisteredException("the identifier [" + identifier + "] is already registered");
		} catch (IdentifierNotRegisteredException e) {
		}
		try {
			RegistryEntry entry = new RegistryEntry();
			entry.setKey(key);
			entry.setIdentifier(identifier.serialize());
			em.persist(entry);
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void create(String key, OrtolangObjectIdentifier identifier, String parent) throws RegistryServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException, IdentifierAlreadyRegisteredException {
		logger.log(Level.INFO, "creating key [" + key + "] for OOI [" + identifier + "] and with parent [" + parent + "]");
		try {
			findEntryByKey(key);
			throw new KeyAlreadyExistsException("the key [" + key + "] already exists");
		} catch (KeyNotFoundException e) {
		}
		try {
			findEntryByIdentifier(identifier);
			throw new IdentifierAlreadyRegisteredException("the identifier [" + identifier + "] is already registered");
		} catch (IdentifierNotRegisteredException e) {
		}
		RegistryEntry pentry = null;
		try {
			pentry = findEntryByKey(parent);
		} catch (KeyNotFoundException e) {
			throw new KeyNotFoundException("no entry found for parent [" + key + "]");
		}
		if ( pentry.getChildren() != null ) {
			throw new BranchNotAllowedException("key [" + parent + "] has already a child, branching is not aloowed");
		} 
		try {
			pentry.setChildren(key);
			pentry.setProperty(OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP, "" + System.currentTimeMillis());
			em.merge(pentry);
			RegistryEntry entry = new RegistryEntry();
			entry.setKey(key);
			entry.setIdentifier(identifier.serialize());
			entry.setParent(parent);
			em.persist(entry);
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean hasChildren(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "checking children existence for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.hasChildren();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isHidden(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "checking visibility state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isHidden();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void hide(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "hidding key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			entry.setHidden(true);
			em.merge(entry);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void show(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "showing key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			entry.setHidden(false);
			em.merge(entry);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isLocked(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "checking lock state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isLocked();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void lock(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "locking key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			entry.setLocked(true);
			em.merge(entry);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isDeleted(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "checking delete state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isDeleted();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			entry.setDeleted(true);
			em.merge(entry);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "lookup identifier for key [" + key + "]");
		OrtolangObjectIdentifier identifier =  OrtolangObjectIdentifier.deserialize(findEntryByKey(key).getIdentifier());
		return identifier;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String lookup(OrtolangObjectIdentifier identifier) throws RegistryServiceException, IdentifierNotRegisteredException {
		logger.log(Level.INFO, "lookup key for identifier [" + identifier + "]");
		String key = findEntryByIdentifier(identifier).getKey();
		return key;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> list(int offset, int limit, String filter) throws RegistryServiceException {
		logger.log(Level.INFO, "listing keys with offset:" + offset + " and limit:" + limit + " and filter:" + filter);
		if (offset < 0) {
			throw new RegistryServiceException("offset MUST be >= 0");
		}
		if (limit < 1) {
			throw new RegistryServiceException("limit MUST be >= 1");
		}
		StringBuffer pfilter = new StringBuffer();
		if ( filter !=  null && filter.length() > 0 ) {
			 pfilter.append(pfilter);
		} 
		pfilter.append("%");
		TypedQuery<String> query = em.createNamedQuery("listVisibleKeys", String.class).setParameter("filter", pfilter.toString()).setFirstResult(offset).setMaxResults(limit);
		List<String> entries = query.getResultList();
		return entries;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long count(String filter) throws RegistryServiceException {
		logger.log(Level.INFO, "counting keys and filter:" + filter);
		StringBuffer pfilter = new StringBuffer();
		if ( filter !=  null && filter.length() > 0 ) {
			 pfilter.append(pfilter);
		} 
		pfilter.append("%");
		TypedQuery<Long> query = em.createNamedQuery("countVisibleKeys", Long.class).setParameter("filter", pfilter.toString());
		long cpt = query.getSingleResult().longValue();
		return cpt;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void tag(String key, String name) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "tagging key [" + key + "] with tag [" + name + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			if ( !entry.getTags().contains(name) ) {
				RegistryTag tag = em.find(RegistryTag.class, name);
				if ( tag == null ) {
					tag = new RegistryTag(name);
					tag.increase();
					em.persist(tag);
				} else {
					tag.increase();
					em.merge(tag);
				} 
				entry.addTag(name);
				em.merge(entry);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void untag(String key, String name) throws RegistryServiceException, KeyNotFoundException, TagNotFoundException {
		logger.log(Level.INFO, "untagging key [" + key + "] from tag [" + name + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			if ( entry.getTags().contains(name) ) {
				RegistryTag tag = em.find(RegistryTag.class, name);
				if ( tag != null ) {
					tag.decrease();
					em.merge(tag);
				} 
				entry.removeTag(name);
				em.merge(entry);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectTag> taglist() throws RegistryServiceException {
		logger.log(Level.INFO, "listing all tags");
		TypedQuery<RegistryTag> query = em.createNamedQuery("listAllTags", RegistryTag.class);
		List<RegistryTag> rtags = query.getResultList();
		List<OrtolangObjectTag> tags = new ArrayList<OrtolangObjectTag> ();
		for ( RegistryTag rtag : rtags ) {
			tags.add(new OrtolangObjectTag(rtag.getName(), rtag.getWeight()));
		}
		
		return tags;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "setting property [" + name + "] with value [" + value + "] for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			entry.setProperty(name, value);
			em.merge(entry);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException {
		logger.log(Level.INFO, "getting property [" + name + "] for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if (!entry.getProperties().containsKey(name)) {
			throw new PropertyNotFoundException("no property with name [" + name + "] found for key [" + key + "]");
		}
		return entry.getProperties().get(name);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectProperty> getProperties(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting properties for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		List<OrtolangObjectProperty> properties = new ArrayList<OrtolangObjectProperty>();
		for ( Entry<String, String> e : entry.getProperties().entrySet() ) {
			properties.add(new OrtolangObjectProperty(e.getKey(), e.getValue()));
		}
		return properties;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private RegistryEntry findEntryByKey(String key) throws KeyNotFoundException {
		RegistryEntry entry = em.find(RegistryEntry.class, key);

		if (entry == null) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		}
		
		return entry;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private RegistryEntry findEntryByIdentifier(OrtolangObjectIdentifier identifier) throws IdentifierNotRegisteredException {
		List<RegistryEntry> entries = null;

		TypedQuery<RegistryEntry> query = em.createNamedQuery("findEntryByIdentifier", RegistryEntry.class).setParameter("identifier", identifier.serialize()); 
		entries = query.getResultList();
		if ( entries == null || entries.size() == 0 ) {
			throw new IdentifierNotRegisteredException("no entry found with identifier [" + identifier + "]");
		}
		if ( entries.size() > 1 ) {
			logger.log(Level.SEVERE, "the identifier [" + identifier + "] is registered more than once !!");
		}

		return entries.get(0);
	}


}
