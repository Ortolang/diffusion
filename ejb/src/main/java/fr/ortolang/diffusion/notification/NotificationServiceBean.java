package fr.ortolang.diffusion.notification;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import fr.ortolang.diffusion.OrtolangEvent;

@Local(NotificationService.class)
@Stateless(name = NotificationService.SERVICE_NAME)
public class NotificationServiceBean implements NotificationService {
	
	private Logger logger = Logger.getLogger(NotificationServiceBean.class.getName());
	
	@Resource(mappedName = "java:jboss/jms/topic/notification")
	private Topic notificationTopic;
	@Inject
	private JMSContext context;
	
	public NotificationServiceBean() {
	}

	@Override
	public void throwEvent(String fromObject, String throwedBy, String objectType, String eventType, String args) throws NotificationServiceException {
		try {
			Message message = context.createMessage();
			message.setStringProperty(OrtolangEvent.DATE, OrtolangEvent.getEventDateFormatter().format(new Date()));
			message.setStringProperty(OrtolangEvent.THROWED_BY, throwedBy);
			message.setStringProperty(OrtolangEvent.FROM_OBJECT, fromObject);
			message.setStringProperty(OrtolangEvent.OBJECT_TYPE, objectType);
			message.setStringProperty(OrtolangEvent.TYPE, eventType);
			message.setStringProperty(OrtolangEvent.ARGUMENTS, args);
			context.createProducer().send(notificationTopic, message);                   
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to throw event", e);
			throw new NotificationServiceException("unable to throw event", e);
		}
	}
}
