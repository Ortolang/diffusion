package fr.ortolang.diffusion.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectTag;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.TagNotFoundException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;

@Remote(BrowserService.class)
@Stateless(name = BrowserService.SERVICE_NAME)
public class BrowserServiceBean implements BrowserService {
	
	private Logger logger = Logger.getLogger(BrowserServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	
	public BrowserServiceBean() {
	}

	@Override
	public OrtolangObjectIdentifier lookup(String key) throws BrowserServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "looking up identifier for key [" + key + "]");
		try {
			RegistryEntry entry = registry.lookup(key); 
			return entry.getIdentifier();
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("unable to lookup identifier for key [" + key + "]", e);
		}
	}

	@Override
	public List<String> list(int offset, int limit, String service, String type) throws BrowserServiceException {
		logger.log(Level.INFO, "listing keys");
		try {
			List<RegistryEntry> entries = registry.list(offset, limit, OrtolangObjectIdentifier.buildFilterPattern(service, type), true);
			List<String> keys = new ArrayList<String>();
			for ( RegistryEntry entry : entries ) {
				keys.add(entry.getKey());
			}
			return keys;
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during listing keys", e);
		}
	}
	
	@Override
	public long count(String service, String type) throws BrowserServiceException {
		logger.log(Level.INFO, "counting identifiers");
		try {
			return registry.count(OrtolangObjectIdentifier.buildFilterPattern(service, type), true);
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during couting identifiers", e);
		}
	}

	@Override
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "getting state for key [" + key + "]");
		try {
			return OrtolangObjectState.fromEntry(registry.lookup(key));
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during getting state", e);
		}
	}

	@Override
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "listing properties for key [" + key + "]");
		try {
			RegistryEntry entry = registry.lookup(key);
			List<OrtolangObjectProperty> properties = new ArrayList<OrtolangObjectProperty>();
			for ( String name : entry.getProperties().keySet() ) {
				properties.add(new OrtolangObjectProperty(name, entry.getProperties().get(name)));
			}
			return properties;
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during listing entries", e);
		}
	}

	@Override
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException,
			PropertyNotFoundException {
		logger.log(Level.INFO, "getting property with name [" + name + "] for key [" + key + "]");
		try {
			RegistryEntry entry = registry.lookup(key);
			if ( entry.getProperties().containsKey(name) ) {
				return new OrtolangObjectProperty(name, entry.getProperties().get(name));
			}
			throw new PropertyNotFoundException("unable to find a property with name [" + name + "] for key[" + key + "]");
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during getting property", e);
		}
	}

	@Override
	public void setProperty(String key, String name, String value) throws BrowserServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "setting property with name [" + name + "] for key [" + key + "]");
		try {
			RegistryEntry entry = registry.lookup(key);
			entry.setProperty(name, value);
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during setting property", e);
		}
	}
	
	@Override
	public OrtolangObjectVersion getVersion(String key) throws BrowserServiceException, KeyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrtolangObjectVersion> history(String key) throws BrowserServiceException, KeyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrtolangObjectTag> listAllTags() throws BrowserServiceException, KeyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrtolangObjectTag> listTags(String key) throws BrowserServiceException, KeyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTag(String key, String name) throws BrowserServiceException, KeyNotFoundException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public OrtolangObjectTag getTag(String name) throws BrowserServiceException, TagNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTag(String key, String name) throws BrowserServiceException, KeyNotFoundException, TagNotFoundException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getServiceName() {
		return BrowserService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return BrowserService.OBJECT_TYPE_LIST;
	}
	
	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

}
