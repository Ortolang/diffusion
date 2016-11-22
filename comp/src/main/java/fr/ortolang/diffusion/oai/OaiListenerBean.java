package fr.ortolang.diffusion.oai;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.oai.format.OAI_DC;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

@MessageDriven(name = "OaiListener", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
		@ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype = 'publication.workspace.publish-snapshot'") 
})
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
	@EJB
	private HandleStoreService handleStore;

	@Override
	@PermitAll
	public void onMessage(Message message) {
		OrtolangEvent event = new Event();
		try {
			event.fromJMSMessage(message);
//			if (event.getType().equals(EVENT_PUBLISH)) {
				OrtolangObjectIdentifier identifier = registry.lookup(event.getFromObject());

				if (identifier.getService().equals(CoreService.SERVICE_NAME) && identifier.getType().equals(Workspace.OBJECT_TYPE)) {
					String wskey = event.getFromObject();
					LOGGER.log(Level.FINE, "creating OAI record for workspace " + wskey);
					oai.createRecord(wskey, MetadataFormat.OAI_DC, registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey));
				}
//			}
		} catch (OrtolangException | RegistryServiceException | KeyNotFoundException | OaiServiceException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private String buildXMLFromWorkspace(String wskey) throws OaiServiceException {
		try {
			Workspace workspace = core.readWorkspace(wskey);
			String snpashot = core.findWorkspaceLatestPublishedSnapshot(wskey);
			String root = workspace.findSnapshotByName(snpashot).getKey();
			
			OrtolangIndexableObject<IndexableJsonContent> indexableObject = OrtolangIndexableObjectFactory.buildJsonIndexableObject(root);
			String item = indexableObject.getContent().getStream().get(MetadataFormat.ITEM);
			
			LOGGER.log(Level.FINE, item);
			
			OAI_DC oai_dc = OAI_DCFactory.buildFromItem(item);
			List<String> handles;
	        try {
	            handles = handleStore.listHandlesForKey(root);
	            for(String handle : handles) {          
	                oai_dc.addDcField("identifier", 
	                        "http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
	                        + "/" +handle);
	            }
	        } catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
	            LOGGER.log(Level.WARNING, "No handle for key " + root, e);
	        }
			return oai_dc.toString();
		} catch (CoreServiceException | KeyNotFoundException | OrtolangException | NotIndexableContentException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from workspace  " + wskey, e);
		}
		throw new OaiServiceException("unable to build oai_dc");
	}

}
