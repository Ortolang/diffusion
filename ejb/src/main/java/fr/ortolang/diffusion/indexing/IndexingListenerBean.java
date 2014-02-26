package fr.ortolang.diffusion.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import fr.ortolang.diffusion.store.index.IndexStoreService;

@MessageDriven(name = "IndexingTopicMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class IndexingListenerBean implements MessageListener {
	
	private Logger logger = Logger.getLogger(IndexingListenerBean.class.getName());
	
	@EJB
	private IndexStoreService store;
	
	public void setIndexStoreService(IndexStoreService store) {
		this.store = store;
	}
	
	public IndexStoreService getIndexStoreService() {
		return store;
	}
	
	@Override
    public void onMessage(Message message) {
		try {
			String action = message.getStringProperty("action");
			String key = message.getStringProperty("key");
			logger.log(Level.INFO, action + " action called on key: " + key);
			//TODO recover the indexable content and pass it to the index store
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to index content", e);
		}
	}

}
