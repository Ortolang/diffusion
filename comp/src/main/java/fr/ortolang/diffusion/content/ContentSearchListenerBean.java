package fr.ortolang.diffusion.content;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.event.entity.Event;

@MessageDriven(name = "ContentSearchListener", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "eventtype = 'publication.workspace.publish-snapshot'") })
@SecurityDomain("ortolang")
public class ContentSearchListenerBean implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(ContentSearchListenerBean.class.getName());

    @EJB
    private ContentSearchServiceWorker worker;
    
    @Override
    @PermitAll
    public void onMessage(Message message) {
        OrtolangEvent event = new Event();
        try {
            event.fromJMSMessage(message);
            if (event.getArguments().containsKey("snapshot")) {
                String wskey = event.getFromObject();
                String snapshot = event.getArguments().get("snapshot");
                HashMap<String, String> args = new HashMap<String, String>();
                args.put("snapshot", snapshot);
                worker.submit(wskey, ContentSearchServiceWorker.INDEX_ACTION, args);
            } else {
                LOGGER.log(Level.SEVERE, "unable to index the content of all document of a workspace without specifying a snapshot");
            }
        } catch (OrtolangException e) {
            LOGGER.log(Level.SEVERE, "unable to index the content of all document of a workspace when receiving publication event for a snapshot", e);
        }
    }
}
