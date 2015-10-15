package fr.ortolang.diffusion.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.event.entity.Event;

@MessageDriven(name = "EventListener", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class EventListenerBean implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(EventListenerBean.class.getName());
    
    @EJB
    private EventService service;

    @Override
    @PermitAll
    public void onMessage(Message message) {
        try {
            Event e = new Event();
            e.fromJMSMessage(message);
            service.persistEvent(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to persist event", e);
        }
    }

}
