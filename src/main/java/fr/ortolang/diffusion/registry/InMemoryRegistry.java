package fr.ortolang.diffusion.registry;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.DiffusionObjectIdentifier;

public class InMemoryRegistry implements RegistryService {
	
	private static InMemoryRegistry instance;
	
	private Logger logger = Logger.getLogger(InMemoryRegistry.class.getName());	
	private HashMap<String, DiffusionObjectIdentifier> registry;
	
	private InMemoryRegistry () {
		registry = new HashMap<String, DiffusionObjectIdentifier>();
	}
	
	public static InMemoryRegistry getInstance() {
		if ( instance == null ) {
			instance = new InMemoryRegistry();
		}
		return instance;
	}

	@Override
	public void bind(String key, DiffusionObjectIdentifier identifier) throws RegistryServiceException, KeyAlreadyBoundException {
		logger.log(Level.INFO, "bind key [" + key + "] to object [" + identifier + "]");
		if ( registry.containsKey(key) ) {
			throw new KeyAlreadyBoundException("key [" + key + "] is already bound");
		} else {
			registry.put(key, identifier);
		}
	}

	@Override
	public void rebind(String key, DiffusionObjectIdentifier identifier) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "rebind key [" + key + "] to object [" + identifier + "]");
		if ( !registry.containsKey(key) ) {
			throw new KeyNotFoundException("key [" + key + "] does not exists");
		} else {
			registry.put(key, identifier);
		}
	}

	@Override
	public void unbind(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "unbind key [" + key + "]");
		if ( !registry.containsKey(key) ) {
			throw new KeyNotFoundException("key [" + key + "] does not exists");
		} else {
			registry.remove(key);
		}
	}

	@Override
	public DiffusionObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "lookup key [" + key + "]");
		if ( !registry.containsKey(key) ) {
			throw new KeyNotFoundException("key [" + key + "] does not exists");
		} else {
			return registry.get(key);
		}
	}

}
