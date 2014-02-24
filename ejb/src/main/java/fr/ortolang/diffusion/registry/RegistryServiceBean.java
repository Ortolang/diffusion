package fr.ortolang.diffusion.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Stateless;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.registry.entity.RegistryTag;

@Local(RegistryService.class)
@Stateless(name = RegistryService.SERVICE_NAME)
public class RegistryServiceBean implements RegistryService {

	private Logger logger = Logger.getLogger(RegistryServiceBean.class.getName());
	private HashMap<String, RegistryEntry> entries;
	private HashMap<String, RegistryTag> tags;

	public RegistryServiceBean() {
		entries = new HashMap<String, RegistryEntry>();
		tags =  new HashMap<String, RegistryTag>();
	}
	
	@Override
	public void create(String key, OrtolangObjectIdentifier identifier) throws RegistryServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "creating registry entry with key [" + key + "] for OOI [" + identifier + "]");
		if (entries.containsKey(key)) {
			throw new KeyAlreadyExistsException("entry already exists for key [" + key + "]");
		} else {
			RegistryEntry entry = new RegistryEntry(key, identifier);
			entries.put(entry.getKey(), entry);
		}
	}

	@Override
	public void create(String key, OrtolangObjectIdentifier identifier, String parent) throws RegistryServiceException, KeyAlreadyExistsException, KeyNotFoundException, BranchNotAllowedException {
		logger.log(Level.INFO, "creating registry entry with key [" + key + "] for OOI [" + identifier + "] and with parent [" + parent + "]");
		if (entries.containsKey(key)) {
			throw new KeyAlreadyExistsException("entry already exists for key [" + key + "]");
		} else {
			if (!entries.containsKey(key)) {
				throw new KeyNotFoundException("no entry found for parent [" + key + "]");
			} else if ( entries.get(parent).getChildren() != null ) {
				throw new BranchNotAllowedException("parent entry with key [" + parent + "] has already a child, branching is not aloowed");
			} else {
				entries.get(parent).setChildren(key);
				RegistryEntry entry = new RegistryEntry(key, identifier);
				entry.setParent(parent);
				entries.put(entry.getKey(), entry);
			}
		}
	}
	
	@Override
	public void hide(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "hidding registry entry for key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			entries.get(key).setHidden(true);
		}
	}

	@Override
	public void show(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "showing registry entry for key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			entries.get(key).setHidden(false);
		}
	}

	@Override
	public void lock(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "locking registry entry for key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			entries.get(key).setLocked(true);
		}
	}

	@Override
	public void delete(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "deleting registry entry for key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			entries.get(key).setDeleted(true);
		}
	}

	@Override
	public RegistryEntry lookup(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "looking up entry for key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			return entries.get(key);
		}
	}

	@Override
	public List<RegistryEntry> list(int offset, int limit, String filter, boolean visible) throws RegistryServiceException {
		logger.log(Level.INFO, "listing " + ((visible)?"all":"only visibles") + " entries with offset:" + offset + " and limit:" + limit + " and filter:" + filter);
		if (offset < 0) {
			throw new RegistryServiceException("offset MUST be >= 0");
		}
		if (limit < -1) {
			throw new RegistryServiceException("limit MUST be >= -1");
		}
		ArrayList<RegistryEntry> rentries = new ArrayList<RegistryEntry>();
		int cpt = 0;
		for (RegistryEntry entry : entries.values()) {
			if ( entry.getIdentifier().serialize().matches(filter) ) {
				if (!visible) {
					if (cpt >= offset) {
						rentries.add(entry);
					}
					cpt++;
				} else if ( !entry.isDeleted() && !entry.isHidden() ) {
					if (cpt >= offset) {
						rentries.add(entry);
					}
					cpt++;
				}
			}
			if (rentries.size() >= limit) {
				break;
			}
		}
		return rentries;
	}

	@Override
	public long count(String filter, boolean visible) throws RegistryServiceException {
		logger.log(Level.INFO, "counting " + ((visible)?"all":"only visibles") + " entries and filter:" + filter);
		int cpt = 0;
		for (RegistryEntry entry : entries.values()) {
			if ( entry.getIdentifier().serialize().matches(filter) ) {
				if ( !visible ) {
					cpt++;
				} else if (!entry.isDeleted() && !entry.isHidden() ) {
					cpt++;
				}
			}
		}
		return cpt;
	}

	@Override
	public void tag(String key, String name) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "tagging entry with key [" + key + "] with tag [" + name + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			if ( !entries.get(key).getTags().contains(name) ) {
				if ( !tags.containsKey(name) ) {
					RegistryTag tag = new RegistryTag(name);
					tags.put(name, tag);
				}
				tags.get(name).increase();
				entries.get(key).addTag(name);
			}
		}
	}

	@Override
	public void untag(String key, String name) throws RegistryServiceException, KeyNotFoundException, TagNotFoundException {
		logger.log(Level.INFO, "untagging entry with key [" + key + "] from tag [" + name + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		}
		if (!tags.containsKey(name)) {
			throw new TagNotFoundException("no tag exists with name [" + name + "]");
		}
		if ( entries.get(key).getTags().contains(name) ) {
			tags.get(name).decrease();
			entries.get(key).removeTag(name);
		} 
	}

	@Override
	public List<RegistryTag> taglist() throws RegistryServiceException {
		ArrayList<RegistryTag> rtags = new ArrayList<RegistryTag>();
		for ( RegistryTag tag : tags.values() ) {
			rtags.add(tag);
		}
		return rtags;
	}
	
	@Override
	public void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "setting property [" + name + "] with value [" + value + "] for entry with key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		} else {
			entries.get(key).setProperty(name, value);
		}
	}
	
	@Override
	public String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException {
		logger.log(Level.INFO, "getting property [" + name + "] for entry with key [" + key + "]");
		if (!entries.containsKey(key)) {
			throw new KeyNotFoundException("no entry found for key [" + key + "]");
		}
		if (!entries.get(key).getProperties().containsKey(name)) {
			throw new PropertyNotFoundException("no property founf with name [" + name + "] found for key [" + key + "]");
		}
		return entries.get(key).getProperties().get(name);
	}

}
