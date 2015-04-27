package fr.ortolang.diffusion.registry;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;

@Local(RegistryService.class)
@Stateless(name = RegistryService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class RegistryServiceBean implements RegistryService {

	private static final  Logger LOGGER = Logger.getLogger(RegistryServiceBean.class.getName());
	
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
	public void register(String key, OrtolangObjectIdentifier identifier, String author) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException {
		LOGGER.log(Level.FINE, "creating key [" + key + "] for OOI [" + identifier + "]");
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
			long current = System.currentTimeMillis();
			RegistryEntry entry = new RegistryEntry();
			entry.setKey(key);
			entry.setIdentifier(identifier.serialize());
			entry.setAuthor(author);
			entry.setCreationDate(current);
			entry.setLastModificationDate(current);
			em.persist(entry);
		} catch ( Exception e ) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void register(String key, OrtolangObjectIdentifier identifier, String parent, boolean inherit) throws RegistryServiceException, KeyAlreadyExistsException, KeyNotFoundException, IdentifierAlreadyRegisteredException {
		LOGGER.log(Level.FINE, "creating key [" + key + "] for OOI [" + identifier + "] and with parent [" + parent + "]");
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
			throw new RegistryServiceException("a newer version already exists for this parent and branching is not permitted");
		}
		
		try {
			pentry.setChildren(key);
			em.merge(pentry);
			RegistryEntry entry = new RegistryEntry();
			entry.setKey(key);
			entry.setIdentifier(identifier.serialize());
			entry.setParent(parent);
			entry.setAuthor(pentry.getAuthor());
			entry.setItem(pentry.isItem());
			entry.setCreationDate(pentry.getCreationDate());
			entry.setLastModificationDate(System.currentTimeMillis());
			if ( inherit ) {
				entry.setPropertiesContent(pentry.getPropertiesContent());
			}
			em.persist(entry);
		} catch ( Exception e ) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean hasChildren(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "checking children existence for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getChildren() != null;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getChildren(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting children for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getChildren();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getParent(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting parent for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getParent();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isHidden(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "checking visibility state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isHidden();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isItem(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "checking if key [" + key + "] is an item");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isItem();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void update(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "updating key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked and cannot be updated");
		}
		try {
			entry.setLastModificationDate(System.currentTimeMillis());
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long getCreationDate(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting creation date for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getCreationDate();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long getLastModificationDate(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting last modification date for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getLastModificationDate();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getAuthor(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting author for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getAuthor();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void hide(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "hidding key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked and cannot be hide");
		}
		try {
			entry.setHidden(true);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void show(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "showing key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked and cannot be shown");
		}
		try {
			entry.setHidden(false);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean isLocked(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "checking lock state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.isLocked();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getLock(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting lock owner for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		return entry.getLock();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void lock(String key, String owner) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "locking key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is already locked");
		}
		try {
			entry.setLock(owner);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void itemify(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "itemify key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked and cannot be itemified");
		}
		try {
			entry.setItem(true);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getPublicationStatus(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting state for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key, LockModeType.PESSIMISTIC_WRITE);
		return entry.getPublicationStatus();
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setPublicationStatus(String key, String state) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "setting key [" + key + "] with state [" + state + "]");
		RegistryEntry entry = findEntryByKey(key, LockModeType.PESSIMISTIC_WRITE);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked, publication status cannot be modified");
		}
		try {
			entry.setPublicationStatus(state);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		delete(key, false);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void delete(String key, boolean force) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "deleting key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( !force && entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked and cannot be deleted");
		}
		try {
			entry.setDeleted(true);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "lookup identifier for key [" + key + "]");
		OrtolangObjectIdentifier identifier =  OrtolangObjectIdentifier.deserialize(findEntryByKey(key).getIdentifier());
		return identifier;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String lookup(OrtolangObjectIdentifier identifier) throws RegistryServiceException, IdentifierNotRegisteredException {
		LOGGER.log(Level.FINE, "lookup key for identifier [" + identifier + "]");
		String key = findEntryByIdentifier(identifier).getKey();
		return key;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> list(int offset, int limit, String identifierFilter, OrtolangObjectState.Status statusFilter, boolean itemFilter) throws RegistryServiceException {
		LOGGER.log(Level.FINE, "listing keys with offset:" + offset + " and limit:" + limit + " and identifierFilter:" + identifierFilter + " and statusFilter: " + statusFilter + " and itemFilter:" + itemFilter);
		if (offset < 0) {
			throw new RegistryServiceException("offset MUST be >= 0");
		}
		if (limit < 1) {
			throw new RegistryServiceException("limit MUST be >= 1");
		}
		StringBuilder ifilter = new StringBuilder();
		if ( identifierFilter !=  null && identifierFilter.length() > 0 ) {
			 ifilter.append(identifierFilter);
		} 
		ifilter.append("%");
		StringBuilder sfilter = new StringBuilder();
		if ( statusFilter != null ) {
			sfilter.append(statusFilter.value());
		} else {
			sfilter.append("%");
		}
		TypedQuery<String> query;
		if ( itemFilter ) {
			LOGGER.log(Level.FINE, "listing items only with identifierFilter: " + ifilter.toString() + " and statusFilter: " + sfilter.toString());
			query = em.createNamedQuery("listVisibleItems", String.class).setParameter("identifierFilter", ifilter.toString()).setParameter("statusFilter", sfilter.toString()).setFirstResult(offset).setMaxResults(limit);
		} else {
			LOGGER.log(Level.FINE, "listing all keys only with identifierFilter: " + ifilter.toString() + " and statusFilter: " + sfilter.toString());
			query = em.createNamedQuery("listVisibleKeys", String.class).setParameter("identifierFilter", ifilter.toString()).setParameter("statusFilter", sfilter.toString()).setFirstResult(offset).setMaxResults(limit);
		}
		List<String> entries = query.getResultList();
		return entries;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long count(String identifierFilter, OrtolangObjectState.Status statusFilter, boolean itemFilter) throws RegistryServiceException {
		LOGGER.log(Level.FINE, "counting keys with identifierFilter:" + identifierFilter + " and statusFilter: " + statusFilter + " and itemFilter:" + itemFilter);
		StringBuilder ifilter = new StringBuilder();
		if ( identifierFilter !=  null && identifierFilter.length() > 0 ) {
			 ifilter.append(identifierFilter);
		} 
		ifilter.append("%");
		StringBuilder sfilter = new StringBuilder();
		if ( statusFilter != null ) {
			sfilter.append(statusFilter.value());
		} else {
			sfilter.append("%");
		}
		TypedQuery<Long> query;
		if ( itemFilter ) {
			query = em.createNamedQuery("countVisibleItems", Long.class).setParameter("identifierFilter", ifilter.toString()).setParameter("statusFilter", sfilter.toString());
		} else {
			query = em.createNamedQuery("countVisibleKeys", Long.class).setParameter("identifierFilter", ifilter.toString()).setParameter("statusFilter", sfilter.toString());
		}
		long cpt = query.getSingleResult().longValue();
		return cpt;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
		LOGGER.log(Level.FINE, "setting property [" + name + "] with value [" + value + "] for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		if ( entry.isLocked() ) {
			throw new KeyLockedException("Key [" + key + "] is locked, property cannot be setted");
		}
		try {
			entry.setProperty(name, value);
			em.merge(entry);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			ctx.setRollbackOnly();
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException {
		LOGGER.log(Level.FINE, "getting property [" + name + "] for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		try {
			if (!entry.getProperties().containsKey(name)) {
				throw new PropertyNotFoundException("no property with name [" + name + "] found for key [" + key + "]");
			}
			return (String) entry.getProperties().get(name);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw new RegistryServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangObjectProperty> getProperties(String key) throws RegistryServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "getting properties for key [" + key + "]");
		RegistryEntry entry = findEntryByKey(key);
		List<OrtolangObjectProperty> properties = new ArrayList<OrtolangObjectProperty>();
		try {
			Properties props = entry.getProperties();
			for ( String name : props.stringPropertyNames() ) {
				properties.add(new OrtolangObjectProperty(name, props.getProperty(name)));
			}
			return properties;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw new RegistryServiceException(e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private RegistryEntry findEntryByKey(String key) throws KeyNotFoundException {
		RegistryEntry entry = em.find(RegistryEntry.class, key);
		if ( entry == null ) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		}
		if ( entry.isDeleted() ) {
			throw new KeyNotFoundException("entry for key [" + key + "] has been deleted");
		}
		
		return entry;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private RegistryEntry findEntryByKey(String key, LockModeType lock) throws KeyNotFoundException {
		RegistryEntry entry = em.find(RegistryEntry.class, key, lock);
		if ( entry == null ) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		}
		if ( entry.isDeleted() ) {
			throw new KeyNotFoundException("entry for key [" + key + "] has been deleted");
		}
		
		return entry;
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private RegistryEntry findEntryByIdentifier(OrtolangObjectIdentifier identifier) throws IdentifierNotRegisteredException {
		List<RegistryEntry> entries = null;

		TypedQuery<RegistryEntry> query = em.createNamedQuery("findEntryByIdentifier", RegistryEntry.class).setParameter("identifier", identifier.serialize()); 
		entries = query.getResultList();
		if ( entries == null || entries.size() == 0 ) {
			throw new IdentifierNotRegisteredException("no entry found with identifier [" + identifier + "]");
		}
		if ( entries.size() > 1 ) {
			LOGGER.log(Level.SEVERE, "the identifier [" + identifier + "] is registered more than once !!");
		}
		if ( entries.get(0).isDeleted() ) {
			throw new IdentifierNotRegisteredException("no entry found with identifier [" + identifier + "]");
		}
		return entries.get(0);
	}

}
