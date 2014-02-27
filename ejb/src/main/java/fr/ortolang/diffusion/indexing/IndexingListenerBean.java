package fr.ortolang.diffusion.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import fr.ortolang.diffusion.OrtolangIndexableContent;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreService;

@MessageDriven(name = "IndexingTopicMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class IndexingListenerBean implements MessageListener {
	
	private Logger logger = Logger.getLogger(IndexingListenerBean.class.getName());
	
	@EJB
	private IndexStoreService store;
	@EJB
	private RegistryService registry;
	
	public void setIndexStoreService(IndexStoreService store) {
		this.store = store;
	}
	
	public IndexStoreService getIndexStoreService() {
		return store;
	}
	
	public RegistryService getRegistry() {
		return registry;
	}

	public void setRegistry(RegistryService registry) {
		this.registry = registry;
	}

	@Override
    public void onMessage(Message message) {
		try {
			String action = message.getStringProperty("action");
			String key = message.getStringProperty("key");
			logger.log(Level.INFO, action + " action called on key: " + key);
			try {
        		if (action.equals("index"))
	                this.addToIndexStore(key);
	            if (action.equals("reindex"))
	                this.updateIndexStore(key);
	            if (action.equals("remove"))
	                this.removeFromIndexStore(key);
        	} catch (Exception e) {
        		logger.log(Level.WARNING, "error during indexation of key " + key, e);
            }
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to index content", e);
		}
	}
	
	private void addToIndexStore(String key) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key);
			store.index(object);
		} catch ( IndexStoreServiceException e ) {
			throw new IndexingServiceException("unable to insert object in index store", e);
		}
	}
	
	private void updateIndexStore(String key) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key);
			store.reindex(key, object);
		} catch ( IndexStoreServiceException e ) {
			throw new IndexingServiceException("unable to update object in index store", e);
		}
	}
	
	private void removeFromIndexStore(String key) throws IndexingServiceException {
		try {
			store.remove(key);
		} catch ( IndexStoreServiceException e ) {
			throw new IndexingServiceException("unable to remove object from index store", e);
		}
	}
	
	private OrtolangIndexableObject buildIndexableObject(String key) throws IndexingServiceException {
		try {
			RegistryEntry entry = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(entry.getIdentifier().getService());
			OrtolangIndexableContent content = service.getIndexableContent(key);
			OrtolangIndexableObject iobject = new OrtolangIndexableObject();
			iobject.setKey(key);
			iobject.setIdentifier(entry.getIdentifier());
			iobject.setService(entry.getIdentifier().getService());
			iobject.setType(entry.getIdentifier().getType());
			iobject.setName(key);
			//TODO remove the name
			iobject.setContent(content);
			return iobject;
		} catch ( Exception e ) {
			throw new IndexingServiceException("unable to get indexable contentn for object ", e);
		}
	}
	
}
