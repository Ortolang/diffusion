package fr.ortolang.diffusion.core.preview;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.event.entity.Event;

@MessageDriven(name = "PreviewMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype LIKE 'core.object.create' OR eventtype LIKE 'core.object.update'")})
@SecurityDomain("ortolang")
public class PreviewListenerBean implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(PreviewListenerBean.class.getName());

	@EJB
	private PreviewServiceWorker worker;
	
	@Override
	@PermitAll
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void onMessage(Message message) {
		try {
			LOGGER.log(Level.FINE, "event received");
			Event e = new Event();
			e.fromJMSMessage(message);
			worker.submit(e.getFromObject(), "generate");
		} catch (OrtolangException | PreviewServiceException e) {
			LOGGER.log(Level.WARNING, "unable to handle event", e);
		}
	}

}
