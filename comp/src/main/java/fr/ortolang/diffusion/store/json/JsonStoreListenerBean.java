package fr.ortolang.diffusion.store.json;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

@MessageDriven(name = "JsonIndexingTopicMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class JsonStoreListenerBean implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreListenerBean.class.getName());

	@EJB
	private JsonStoreService store;
	@EJB
	private RegistryService registry;

	public void setJsonStoreService(JsonStoreService store) {
		this.store = store;
	}

	public JsonStoreService getJsonStoreService() {
		return store;
	}

	public RegistryService getRegistry() {
		return registry;
	}

	public void setRegistry(RegistryService registry) {
		this.registry = registry;
	}

	@Override
	@PermitAll
	public void onMessage(Message message) {
		try {
			String action = message.getStringProperty("action");
			String key = message.getStringProperty("key");
			LOGGER.log(Level.FINE, action + " action called on key: " + key);
			try {
				if (action.equals("index"))
					this.addToStore(key);
				if (action.equals("reindex"))
					this.updateStore(key);
				if (action.equals("remove"))
					this.removeFromStore(key);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "error during indexation of key " + key, e);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "unable to index content", e);
		}
	}

	private void addToStore(String key) throws IndexingServiceException {
		try {
			String status = registry.getPublicationStatus(key);
			if ( status.equals(OrtolangObjectState.Status.PUBLISHED) ) {
				LOGGER.log(Level.FINE, "key " + key + " is state " + OrtolangObjectState.Status.PUBLISHED + ", indexing in json store");
				OrtolangIndexableObject<IndexableJsonContent> object = OrtolangIndexableObjectFactory.buildJsonIndexableObject(key);
				store.index(object);
			}
		} catch (JsonStoreServiceException | OrtolangException | RegistryServiceException | KeyNotFoundException e) {
			throw new IndexingServiceException("unable to insert object in store", e);
		}
	}

	private void updateStore(String key) throws IndexingServiceException {
		try {
			String status = registry.getPublicationStatus(key);
			if ( status.equals(OrtolangObjectState.Status.PUBLISHED) ) {
				LOGGER.log(Level.FINE, "key " + key + " is state " + OrtolangObjectState.Status.PUBLISHED + ", reindexing in json store");
				OrtolangIndexableObject<IndexableJsonContent> object = OrtolangIndexableObjectFactory.buildJsonIndexableObject(key);
				store.reindex(object);
			}
		} catch (JsonStoreServiceException | OrtolangException | RegistryServiceException | KeyNotFoundException e) {
			throw new IndexingServiceException("unable to update object in store", e);
		}
	}

	private void removeFromStore(String key) throws IndexingServiceException {
		try {
			LOGGER.log(Level.FINE, "key " + key + " removed from json store");
			store.remove(key);
		} catch (JsonStoreServiceException e) {
			throw new IndexingServiceException("unable to remove object from store", e);
		}
	}

}
