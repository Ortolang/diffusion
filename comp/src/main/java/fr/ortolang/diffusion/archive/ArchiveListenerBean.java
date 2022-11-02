package fr.ortolang.diffusion.archive;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.logging.Level;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

@MessageDriven(name = "ArchiveListener", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/archive"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class ArchiveListenerBean implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(ArchiveListenerBean.class.getName());

    @EJB
    private ArchiveServiceWorker worker;

    @Override
    @PermitAll
    public void onMessage(Message message) {
        try {
            String key = message.getStringProperty("key");
            String action = message.getStringProperty("action");
            String schema = message.getStringProperty("schema");
            HashMap<String, String> args = new HashMap<>();
            args.put("schema", schema);

            LOGGER.log(Level.FINEST, "Submits Archivable job to the message queue for object with key {0}", key);
            worker.submit(key, action, args);
        } catch (JMSException e) {
            LOGGER.log(Level.WARNING, "Unable to handle checking archivable message", e);
        }
    }
    
}