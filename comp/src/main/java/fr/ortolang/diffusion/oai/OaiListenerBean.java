package fr.ortolang.diffusion.oai;

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
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.registry.RegistryService;

@MessageDriven(name = "OaiListener", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype = 'publication.workspace.publish-snapshot'") 
})
@SecurityDomain("ortolang")
public class OaiListenerBean implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(OaiListenerBean.class.getName());

    @EJB
    private RegistryService registry;
	@EJB
	private CoreService core;
	@EJB
	private OaiService oai;

	@Override
	@PermitAll
	public void onMessage(Message message) {
		OrtolangEvent event = new Event();
		try {
			event.fromJMSMessage(message);
			if (event.getArguments().containsKey("snapshot")) {
				String wskey = event.getFromObject();
				String snapshot = event.getArguments().get("snapshot");
				
				oai.buildRecordsForWorkspace(wskey, snapshot);
			} else {
				LOGGER.log(Level.SEVERE, "unable to create OAI record without specifying a snapshot");
			}
		} catch (OrtolangException | OaiServiceException e) {
			LOGGER.log(Level.SEVERE, "unable to create OAI record when receiving publication event for a snapshot", e);
		}
	}
}
