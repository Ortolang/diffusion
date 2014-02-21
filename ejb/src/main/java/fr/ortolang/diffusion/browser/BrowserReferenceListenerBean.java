package fr.ortolang.diffusion.browser;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven(name = "EventLoggerTopicMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class BrowserReferenceListenerBean implements MessageListener {
	
	private Logger logger = Logger.getLogger(BrowserReferenceListenerBean.class.getName());
	
	@Override
    public void onMessage(Message message) {
		try {
			//TODO update references on dedicated events
		} catch (Exception e) {
			logger.log(Level.WARNING, "error during treating event", e);
		}
	}

}
