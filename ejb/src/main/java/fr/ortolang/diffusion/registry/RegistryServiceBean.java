package fr.ortolang.diffusion.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Stateless;

@Local(RegistryService.class)
@Stateless(name = RegistryService.SERVICE_NAME)
public class RegistryServiceBean implements RegistryService {
	
	private Logger logger = Logger.getLogger(RegistryServiceBean.class.getName());	
	private HashMap<String, RegistryEntry> registry;
	
	public RegistryServiceBean () {
		registry = new HashMap<String, RegistryEntry>();
	}
	
	@Override
	public void create(RegistryEntry entry) throws RegistryServiceException, EntryAlreadyExistsException {
		logger.log(Level.INFO, "creating registry entry: " + entry);
		if ( registry.containsKey(entry.getKey()) ) {
			throw new EntryAlreadyExistsException("entry already exists for key [" + entry.getKey() + "]");
		} else {
			registry.put(entry.getKey(), entry);
		}
	}

	@Override
	public void update(RegistryEntry entry) throws RegistryServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "updating registry entry: " + entry);
		if ( !registry.containsKey(entry.getKey()) ) {
			throw new EntryNotFoundException("no entry found for key [" + entry.getKey() + "]");
		} else {
			registry.put(entry.getKey(), entry);
		}
	}

	@Override
	public void delete(String key) throws RegistryServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "deleting registry entry for key [" + key + "]");
		if ( !registry.containsKey(key) ) {
			throw new EntryNotFoundException("no entry found for key [" + key + "]");
		} else {
			registry.remove(key);
		}
	}

	@Override
	public RegistryEntry lookup(String key) throws RegistryServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "lookup key [" + key + "]");
		if ( !registry.containsKey(key) ) {
			throw new EntryNotFoundException("no entry found for key [" + key + "]");
		} else {
			return registry.get(key);
		}
	}
	
	@Override
	public List<RegistryEntry> list(int offset, int limit) throws RegistryServiceException {
		logger.log(Level.INFO, "list entries offset [" + offset + "], limit [" + limit + "]");
		if ( offset < 0 ) {
			throw new RegistryServiceException("offset MUST be >= 0");
		}
		if ( limit < -1 ) {
			throw new RegistryServiceException("limit MUST be >= -1");
		}
		ArrayList<RegistryEntry> entries = new ArrayList<RegistryEntry> ();
		int cpt = 0;
		for ( RegistryEntry entry : registry.values() ) {
			if ( cpt >= offset && ( entries.size() < limit || limit == -1 )) {
				entries.add(entry);
			}
			cpt++;
		}
		return entries;
	}

}
