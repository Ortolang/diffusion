package fr.ortolang.diffusion.store.json;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

@MessageDriven(name = "JsonIndexingTopicMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class JsonStoreListenerBean implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreListenerBean.class.getName());
	
	@EJB
	private JsonStoreServiceWorker worker;
	
	@Override
	@PermitAll
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void onMessage(Message message) {
		try {
			LOGGER.log(Level.FINE, "indexation message received");
			String action = message.getStringProperty("action");
			String key = message.getStringProperty("key");
			LOGGER.log(Level.FINE, "submitting action to json indexation service worker");
			worker.submit(key, action);
		} catch (JMSException | JsonStoreServiceException e) {
			LOGGER.log(Level.WARNING, "unable to handle indexation message", e);
		}
	}
	
}
