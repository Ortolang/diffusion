package fr.ortolang.diffusion.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import fr.ortolang.diffusion.event.entity.Event;

@MessageDriven(name = "EventLoggerTopicMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class EventLoggerListenerBean implements MessageListener {
	
	private Logger logger = Logger.getLogger(EventLoggerListenerBean.class.getName());
	
	@Override
	public void onMessage(Message message) {
		try {
			Event e = new Event();
			e.fromJMSMessage(message);
			logger.log(Level.INFO, EventLoggerFormater.formatEvent(e));
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to log event", e);
		}
	}

}
