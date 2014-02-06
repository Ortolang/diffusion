package fr.ortolang.diffusion.browser;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.registry.EntryNotFoundException;
import fr.ortolang.diffusion.registry.RegistryEntry;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

@Remote(BrowserService.class)
@Stateless(name = BrowserService.SERVICE_NAME)
public class BrowserServiceBean implements BrowserService {
	
	private Logger logger = Logger.getLogger(BrowserServiceBean.class.getName());
	
	@EJB
	private RegistryService registry;
	
	public BrowserServiceBean() {
	}

	@Override
	public RegistryEntry lookup(String key) throws BrowserServiceException, EntryNotFoundException {
		logger.log(Level.INFO, "lookup registry entry for key [" + key + "]");
		try {
			return registry.lookup(key);
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("unable to lookup key [" + key + "]", e);
		}
	}

	@Override
	public List<RegistryEntry> list(int offset, int limit) throws BrowserServiceException {
		logger.log(Level.INFO, "list registry entries");
		try {
			return registry.list(offset, limit);
		} catch ( RegistryServiceException e) {
			throw new BrowserServiceException("error during listing entries", e);
		}
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
