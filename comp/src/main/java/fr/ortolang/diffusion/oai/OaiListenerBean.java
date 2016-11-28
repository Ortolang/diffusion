package fr.ortolang.diffusion.oai;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.format.DCXMLDocument;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.oai.format.OLACFactory;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
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

    @EJB
    private RegistryService registry;
	@EJB
	private CoreService core;
	@EJB
	private OaiService oai;
	@EJB
	private HandleStoreService handleStore;
	@EJB
	private BinaryStoreService binaryStore;

	@Override
	@PermitAll
	public void onMessage(Message message) {
		OrtolangEvent event = new Event();
		try {
			event.fromJMSMessage(message);
			if (event.getArguments().containsKey("snapshot")) {
				String wskey = event.getFromObject();
				String snapshot = event.getArguments().get("snapshot");
				HashSet<String> setsWorkspace = new HashSet<String>(Arrays.asList(wskey));
				
				List<Record> records = null;
				try {
					records = oai.listRecordsByIdentifier(wskey);
				} catch (RecordNotFoundException e) {
				}
				
				if (records==null) {
					LOGGER.log(Level.FINE, "creating OAI record, set and setRecord for workspace " + wskey + " and snapshot " + snapshot);
					// Creating a Set for the workspace
					try {
						oai.createSet(wskey, "Workspace "+wskey);
					} catch (SetAlreadyExistsException e) {
						LOGGER.log(Level.WARNING, "unable to create a Set " + wskey, e);
					}
					createRecordsForWorkspace(wskey, snapshot, setsWorkspace);
				} else {
					LOGGER.log(Level.FINE, "updating OAI record for workspace " + wskey + " and snapshot " + snapshot);
					// Set is already created (see below), so just cleans and creates new records
					// Deleting all Records linking of the workspace
					List<Record> recordsOfWorkspace;
					try {
						recordsOfWorkspace = oai.listRecordsBySet(wskey);
						recordsOfWorkspace.forEach(rec -> {
							try {
								oai.deleteRecord(rec.getId());
							} catch (RecordNotFoundException e) {
							}
						});
					} catch (RecordNotFoundException e1) {
						// No record
					}
					createRecordsForWorkspace(wskey, snapshot, setsWorkspace);
				}
			} else {
				LOGGER.log(Level.SEVERE, "unable to create OAI record without specifying a snapshot");
			}
		} catch (OrtolangException | RegistryServiceException | KeyNotFoundException | OaiServiceException | CoreServiceException | MetadataPrefixUnknownException e) {
			LOGGER.log(Level.SEVERE, "unable to create OAI record", e);
		}
	}
	
	private void createRecordsForWorkspace(String wskey, String snapshot, HashSet<String> setsWorkspace) throws RegistryServiceException, KeyNotFoundException, OaiServiceException, MetadataPrefixUnknownException, CoreServiceException {

		// Creating a Record for the workspace
		oai.createRecord(wskey, MetadataFormat.OAI_DC, registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey, snapshot, MetadataFormat.OAI_DC), setsWorkspace);
		oai.createRecord(wskey, MetadataFormat.OLAC, registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey, snapshot, MetadataFormat.OLAC), setsWorkspace);
		
		// Creating a Record for each element with OAI_DC metadata object of the workspace
		Map<String, Map<String, List<String>>> map = core.buildWorkspacePublicationMap(wskey, snapshot);
        for(String key : map.keySet()) {
        	String oai_dc = buildXMLFromOrtolangObject(key, MetadataFormat.OAI_DC);
        	if (oai_dc!=null) {
        		oai.createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dc, setsWorkspace);
        	}
        	String olac = buildXMLFromOrtolangObject(key, MetadataFormat.OLAC);
        	if (olac!=null) {
        		oai.createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olac, setsWorkspace);
        	}
        }
	}
	
	private String buildXMLFromWorkspace(String wskey, String snapshot, String metadataPrefix) throws OaiServiceException, MetadataPrefixUnknownException {
		LOGGER.log(Level.FINE, "building XML for workspace " + wskey + " and snapshot " + snapshot + " and metadataPrefix " + metadataPrefix);
		try {
			Workspace workspace = core.readWorkspace(wskey);
			String root = workspace.findSnapshotByName(snapshot).getKey();
			OrtolangIndexableObject<IndexableJsonContent> indexableObject = OrtolangIndexableObjectFactory.buildJsonIndexableObject(root);
			String item = indexableObject.getContent().getStream().get(MetadataFormat.ITEM);

			DCXMLDocument xml = null;
			if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
				xml = OAI_DCFactory.buildFromItem(item);
			} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
				xml = OLACFactory.buildFromItem(item);
			}
			
			// Automatically adds handles to 'identifier' XML element
			List<String> handles;
			try {
				handles = handleStore.listHandlesForKey(root);
				for(String handle : handles) {          
					xml.addDcField("identifier", 
							"http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
							+ "/" +handle);
				}
			} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
				LOGGER.log(Level.WARNING, "No handle for key " + root, e);
			}
			if (xml!=null) {				
				return xml.toString();
			} else {
				throw new MetadataPrefixUnknownException("unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
			}
		} catch (CoreServiceException | KeyNotFoundException | OrtolangException | NotIndexableContentException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from workspace " + wskey, e);
			throw new OaiServiceException("unable to build xml for oai record");
		}
	}
	
	private String buildXMLFromOrtolangObject(String key, String metadataPrefix) throws OaiServiceException {
		LOGGER.log(Level.FINE, "creating OAI record for ortolang object " + key + " for metadataPrefix " + metadataPrefix);
		try {
			List<String> mdKeys = core.findMetadataObjectsForTargetAndName(key, metadataPrefix);
			DCXMLDocument xml = null;

			if (!mdKeys.isEmpty()) {
				String mdKey = mdKeys.get(0);
				MetadataObject md = core.readMetadataObject(mdKey);
				if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
					xml = OAI_DCFactory.buildFromJson(getContent(binaryStore.get(md.getStream())));
				} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
					xml = OLACFactory.buildFromJson(getContent(binaryStore.get(md.getStream())));
				}
			} else {
				return null;
			}
			
			// Automatically adds handles to 'identifier' XML element
			List<String> handles;
			try {
				handles = handleStore.listHandlesForKey(key);
				for(String handle : handles) {          
					xml.addDcField("identifier", 
							"http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
							+ "/" +handle);
				}
			} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
				LOGGER.log(Level.WARNING, "No handle for key " + key, e);
			}
			if (xml!=null) {				
				return xml.toString();
			} else {
				throw new OaiServiceException("unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
			}
		} catch (OrtolangException | KeyNotFoundException | CoreServiceException | IOException | BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from ortolang object  " + key, e);
			throw new OaiServiceException("unable to build xml for oai record");
		}
	}

    private String getContent(InputStream is) throws IOException {
        String content = null;
        try {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "  unable to get content from stream", e);
        } finally {
            is.close();
        }
        return content;
    }

}
