package fr.ortolang.diffusion.preview;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.event.entity.Event;

@MessageDriven(name = "PreviewMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype LIKE 'core.object.create' OR eventtype LIKE 'core.object.update'")})
@SecurityDomain("ortolang")
@PermitAll
@RunAs(PreviewListenerBean.PREVIEW_LISTENER_ROLE)
public class PreviewListenerBean implements MessageListener {

	public static final String PREVIEW_LISTENER_ROLE = "preview";
	
	private static final Logger LOGGER = Logger.getLogger(PreviewListenerBean.class.getName());

	@EJB
	private PreviewService service;
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void onMessage(Message message) {
		try {
			LOGGER.log(Level.FINE, "event received");
			Event e = new Event();
			e.fromJMSMessage(message);
			service.submit(new OrtolangJob("generate", e.getFromObject(), System.currentTimeMillis(), e.getArguments()));
		} catch (OrtolangException | PreviewServiceException e) {
			LOGGER.log(Level.WARNING, "unable to handle event", e);
		}
	}

}
