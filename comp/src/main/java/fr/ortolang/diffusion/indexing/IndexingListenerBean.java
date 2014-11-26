package fr.ortolang.diffusion.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.triple.TripleStoreService;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;

@MessageDriven(name = "IndexingTopicMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
@RunAs("system")
public class IndexingListenerBean implements MessageListener {

	private Logger logger = Logger.getLogger(IndexingListenerBean.class.getName());

	@EJB
	private IndexStoreService indexStore;
	@EJB
	private TripleStoreService tripleStore;
	@EJB
	private RegistryService registry;

	public void setIndexStoreService(IndexStoreService store) {
		this.indexStore = store;
	}

	public IndexStoreService getIndexStoreService() {
		return indexStore;
	}

	public TripleStoreService getTripleStoreService() {
		return tripleStore;
	}

	public void setTripleStoreService(TripleStoreService triple) {
		this.tripleStore = triple;
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
			String root = message.getStringProperty("root");
			String path = message.getStringProperty("path");
			IndexingContext context = new IndexingContext(root, path);
			logger.log(Level.FINE, action + " action called on key: " + key);
			try {
				if (action.equals("index"))
					this.addToStore(key, context);
				if (action.equals("reindex"))
					this.updateStore(key, context);
				if (action.equals("remove"))
					this.removeFromStore(key, context);
			} catch (Exception e) {
				logger.log(Level.WARNING, "error during indexation of key " + key, e);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to index content", e);
		}
	}

	private void addToStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key, context);
			indexStore.index(object);
			tripleStore.index(object);
		} catch (IndexStoreServiceException | TripleStoreServiceException e) {
			throw new IndexingServiceException("unable to insert object in store", e);
		}
	}

	private void updateStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key, context);
			indexStore.reindex(object);
			tripleStore.reindex(object);
		} catch (IndexStoreServiceException | TripleStoreServiceException e) {
			throw new IndexingServiceException("unable to update object in store", e);
		}
	}

	private void removeFromStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			indexStore.remove(key);
			tripleStore.remove(key);
		} catch (IndexStoreServiceException | TripleStoreServiceException e) {
			throw new IndexingServiceException("unable to remove object from store", e);
		}
	}

	private OrtolangIndexableObject buildIndexableObject(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
			OrtolangIndexablePlainTextContent content = service.getIndexablePlainTextContent(key);
			logger.info("indexing content : "+content);
			OrtolangIndexableSemanticContent scontent = service.getIndexableSemanticContent(key);
			OrtolangIndexableObject iobject = new OrtolangIndexableObject();
			iobject.setKey(key);
			iobject.setIdentifier(identifier);
			iobject.setService(identifier.getService());
			iobject.setType(identifier.getType());
			iobject.setHidden(registry.isHidden(key));
			iobject.setStatus(registry.getPublicationStatus(key));
			iobject.setProperties(registry.getProperties(key));
			iobject.setAuthor(registry.getAuthor(key));
			iobject.setCreationDate(registry.getCreationDate(key));
			iobject.setLastModificationDate(registry.getLastModificationDate(key));
			iobject.setName(key);
			iobject.setPlainTextContent(content);
			iobject.setSemanticContent(scontent);
			iobject.setContext(context);
			return iobject;
		} catch (Exception e) {
			throw new IndexingServiceException("unable to get indexable content for object ", e);
		}
	}

}
