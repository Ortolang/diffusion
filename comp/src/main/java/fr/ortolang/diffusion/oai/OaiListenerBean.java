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
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.oai.format.OAI_DC;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

@MessageDriven(name = "OaiListener", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class OaiListenerBean implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(OaiListenerBean.class.getName());

	private static final String EVENT_PUBLISH = Event.buildEventType(PublicationService.SERVICE_NAME,
			OrtolangObject.OBJECT_TYPE, "publish");

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
			if (event.getType().equals(EVENT_PUBLISH)) {
				OrtolangObjectIdentifier identifier = registry.lookup(event.getFromObject());

				if (identifier.getService().equals(CoreService.SERVICE_NAME) && identifier.getType().equals(Workspace.OBJECT_TYPE)) {
					String wskey = event.getFromObject();
					LOGGER.log(Level.FINE, "creating OAI record for workspace : " + wskey);
					oai.createRecord(wskey, "oai_dc", registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey));
				}
			}
		} catch (OrtolangException | RegistryServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private String buildXMLFromWorkspace(String workspace) {
		OAI_DC writer = new OAI_DC();
		
		return writer.toString();
	}

}
