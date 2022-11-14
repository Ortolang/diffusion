package fr.ortolang.diffusion.archive;

import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_CORPUS;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_TERMINOLOGY;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_TOOL_SERVICE;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_WEBSITE;
import static fr.ortolang.diffusion.oai.format.Constant.SIP_NAMESPACE_PREFIX;
import static fr.ortolang.diffusion.oai.format.Constant.SIP_NAMESPACE_SCHEMA_LOCATION;
import static fr.ortolang.diffusion.oai.format.Constant.SIP_NAMESPACE_URI;
import static fr.ortolang.diffusion.oai.format.Constant.OLAC_LINGUISTIC_TYPES;
import static fr.ortolang.diffusion.oai.format.Constant.ORTOLANG_RESOURCE_TYPE_CORPORA;
import static fr.ortolang.diffusion.oai.format.Constant.ORTOLANG_RESOURCE_TYPE_LEXICON;
import static fr.ortolang.diffusion.oai.format.Constant.ORTOLANG_RESOURCE_TYPE_TERMINOLOGY;
import static fr.ortolang.diffusion.oai.format.Constant.ORTOLANG_RESOURCE_TYPE_TOOL;
import static fr.ortolang.diffusion.oai.format.Constant.XSI_NAMESPACE_PREFIX;
import static fr.ortolang.diffusion.oai.format.Constant.XSI_NAMESPACE_URI;
import static fr.ortolang.diffusion.oai.format.Constant.iso639_2pattern;
import static fr.ortolang.diffusion.oai.format.Constant.person;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.tika.metadata.Metadata;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;
import fr.ortolang.diffusion.archive.facile.FacileService;
import fr.ortolang.diffusion.archive.facile.entity.Validator;
import fr.ortolang.diffusion.archive.format.Sip;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStreamFactory;
import fr.ortolang.diffusion.store.binary.hash.MD5FilterInputStreamFactory;
import fr.ortolang.diffusion.util.DateUtils;
import fr.ortolang.diffusion.util.XmlUtils;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.extraction.parser.OrtolangXMLParser;
import fr.ortolang.diffusion.indexing.IndexableContentParsingException;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContentParser;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

import org.activiti.bpmn.converter.IndentingXMLStreamWriter;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Startup
@Local(ArchiveService.class)
@Singleton(name = ArchiveService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ArchiveServiceBean implements ArchiveService {

    private static final Logger LOGGER = Logger.getLogger(ArchiveServiceBean.class.getName());

    public static final String DEFAULT_SIP_HOME = "sip";
    public static final String DEPOT_DIRECTORY = "DEPOT";
    public static final String DESC_DIRECTORY = "DESC";
    public static final String SIP_XML_FILE = "sip.xml";
    public static final String SIP_XML_FILEPATH = "/sip.xml";
    
    @Resource(mappedName = "java:jboss/exported/jms/topic/archive")
    private Topic archiveQueue;
    @Inject
    private JMSContext context;

    @EJB
    private BinaryStoreService binarystore;
	@EJB
    private CoreService core;
    @EJB
    private RegistryService registry;
    @EJB
    private FacileService facile;

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

	private HashedFilterInputStreamFactory factory;
    private Path base;

    /**
     * Creates a new Archive service.
     */
    public ArchiveServiceBean() {
        // No need to initialize
    }

    @PostConstruct
    public void init() {
        this.base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_SIP_HOME);
        LOGGER.log(Level.INFO, "Initializing Archive Service with base directory : {0}" , base);
		this.factory = new MD5FilterInputStreamFactory();
        try {
			Files.createDirectories(base);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to initialize sip directory", e);
		}
    }

    @PreDestroy
    public void shutdown() {
        // Not needed
    }

    @Override
    public String getServiceName() {
        return ArchiveService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    /**
     * Sends to the archive topic the message to check whether the object is archivable.
     */
    public void checkArchivable(String key) throws ArchiveServiceException {
        sendMessage(key, ArchiveService.CHECK_ACTION);
    }

	/**
	 * Sends a message to the archive queue.
	 * @param key the key of the ortolang object
	 * @param action see action availabled to ArchiveService
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void sendMessage(String key, String action) throws ArchiveServiceException {
		try {
            Message message = context.createMessage();
			message.setStringProperty("action", action);
            message.setStringProperty("key", key);
            context.createProducer().send(archiveQueue, message);
        } catch (Exception e) {
            throw new ArchiveServiceException("unable to send archive message", e);
        }
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void validateDataobject(String key) throws ArchiveServiceException {
		OrtolangObjectIdentifier identifier;
		try {
			identifier = registry.lookup(key);
			
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new ArchiveServiceException("unable to get object from registry with key " + key, e);
		}
		if (!identifier.getService().equals(core.getServiceName())
			|| !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
			throw new ArchiveServiceException("unable to validate other than a DataObject");
		}
		DataObject dataObject = em.find(DataObject.class, identifier.getId());
		dataObject.setKey(key);
		try {
			Validator validator = validateFacile(dataObject);
			if (validator != null) {
				String json = writeJson(validator);
				String metadataHash = binarystore
						.put(new ByteArrayInputStream(json.getBytes()));
				core.systemCreateMetadata(dataObject.getKey(), MetadataFormat.FACILE_VALIDATOR, metadataHash,
						MetadataFormat.FACILE_VALIDATOR + ".json");
			} else {
				LOGGER.log(Level.WARNING, "Validator XML cant be parsed for data object {0}", dataObject.getKey());
			}
		} catch(BinaryStoreServiceException | DataNotFoundException | CheckArchivableException | DataCollisionException | JsonProcessingException | KeyNotFoundException | CoreServiceException | MetadataFormatException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | RegistryServiceException | AuthorisationServiceException | IndexingServiceException e) {
			throw new ArchiveServiceException("unable to validate via Facile the dataobject " + key, e);
		}
		
	}

	/**
	 * Creates a Tar archive.
	 * 
	 * After the creation of the archive, the method finishArchive needs to be call.
	 * @param wskey the workspace key
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ArchiveOutputStream createArchive(String wskey) throws ArchiveServiceException {
		ArchiveOutputStream tarOutput = null;
		try {
			tarOutput = new TarArchiveOutputStream(Files.newOutputStream(base.resolve(wskey + ".tar")));
		} catch (IOException e) {
			throw new ArchiveServiceException("unable to create the Tar " + base.resolve(wskey + ".tar"), e);
		}
		return tarOutput;
	}

	/**
	 * Finishes the arvhive.
	 * @param tarOutput the TAR archive
	 * @throws ArchiveServiceException
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void finishArchive(ArchiveOutputStream tarOutput) throws ArchiveServiceException {
		try {
			tarOutput.finish();
		} catch (IOException e) {
			throw new ArchiveServiceException("unable to finish the Tar", e);
		}
	}

	/**
	 * Adds a file to the Sip Archive.
	 * @param wsKey the workspace key
	 * @param snapshot the snapshot to use
	 * @param schema a schema used by some files added to the sip archive
	 * @param archiveEntryList the list of entries to add
	 * @param archive the archive output stream
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public java.nio.file.Path addXmlSIPFileToArchive(String wskey, String snapshot, String schema, List<ArchiveEntry> archiveEntryList, ArchiveOutputStream archive) throws ArchiveServiceException {
		Workspace workspace = null;
		try {
			workspace = core.systemReadWorkspace(wskey);
		} catch (CoreServiceException | KeyNotFoundException e1) {
			throw new ArchiveServiceException("Unable to read workspace of key " + wskey);
		}
		// Get the snapshot or the root collection
		String root = null;
		if ( snapshot == null ) {
			root = workspace.getHead();
		} else {
			if (!workspace.containsSnapshotName(snapshot)) {
				throw new ArchiveServiceException(
						"the workspace with key: " + wskey + " does not contain a snapshot with name: " + snapshot);
			}
			root = workspace.findSnapshotByName(snapshot).getKey();
		}
		MetadataElement itemMetadata;
		try {
			itemMetadata = core.systemReadCollection(root).findMetadataByName(MetadataFormat.ITEM);
		} catch (AccessDeniedException | CoreServiceException | KeyNotFoundException e) {
			throw new ArchiveServiceException("Unable to find metadata by name " + MetadataFormat.ITEM + " of collection " + root, e);
		}
    	if (itemMetadata == null) {
    		throw new ArchiveServiceException("No meta type ITEM to the collection " + root);
    	}

		MetadataObject md = null;
		try {
			md = core.systemReadMetadataObject(itemMetadata.getKey());
		} catch (AccessDeniedException | CoreServiceException | KeyNotFoundException e) {
			throw new ArchiveServiceException("Unable to read metadata object " + itemMetadata.getKey(), e);
		}

		if (md == null) {
    		throw new ArchiveServiceException("Unable to read MetadataObject with key " + itemMetadata.getKey());
    	}

		java.nio.file.Path xmlSipFile = null;
		try {
			xmlSipFile = Files.createTempFile("sip", ".xml");
		} catch (IOException eCreateTempFile) {
			throw new ArchiveServiceException("unexpected error during the creation of the xml sip file", eCreateTempFile);
		}

		try (OutputStream os = Files.newOutputStream(xmlSipFile)) {
			XMLStreamWriter writer = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(os));

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
				
			XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
			namespaces.put(SIP_NAMESPACE_PREFIX, new XmlDumpNamespace(SIP_NAMESPACE_URI, SIP_NAMESPACE_SCHEMA_LOCATION));
			namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
			builder.setNamespaces(namespaces);
			builder.writeStartDocument(SIP_NAMESPACE_PREFIX, Sip.Pac, null);

			createDocDC(wskey, binarystore.get(md.getStream()), writer, builder);
			createDocMeta(workspace.getAlias(), writer, builder);
			createFichMeta(archiveEntryList, writer, builder, schema);

			writer.writeEndElement(); // end of Pac
			writer.writeEndDocument();
			writer.flush();
			writer.close();
			
			// Validate the sip.xml generated by this class
			validateSipXml(xmlSipFile);
		} catch (IOException | XMLStreamException | MetadataBuilderException | BinaryStoreServiceException | DataNotFoundException e) {
			throw new ArchiveServiceException("unable to create SIP XML", e);
		}
		// Adds the xml sip file to the archive
		try (InputStream xmlInputstream = Files.newInputStream(xmlSipFile)) {
			this.addInputstreamToArchive(xmlInputstream, ArchiveEntry.newArchiveEntry(SIP_XML_FILEPATH, 
			SIP_XML_FILE, Files.size(xmlSipFile)), archive);
		} catch(Exception e) {
			throw new ArchiveServiceException("unable to get an inputstream of the SIP XML file", e);
		}

		// Deletes temp file
		try {
			Files.delete(xmlSipFile);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "unexpected error during importation of the xml sip file to the Tar", e);
		}
		return xmlSipFile;
	}

	/**
	 * Validates the XML SIP file.
	 * @param path path to the XML SIP file
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void validateSipXml(Path path) throws ArchiveServiceException {
		try {
			String result = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
	    	if (!result.isEmpty()) {
				XmlUtils.validateXml(result);
			} else {
				throw new ArchiveServiceException("unable to validate xml cause sip.xml file is empty");
			}
		} catch (IOException e) {
			throw new ArchiveServiceException("unable to create SIP XML", e);
		} catch (SAXException e) {
			throw new ArchiveServiceException("sip.xml is not valid", e);
		}
    }

    /**
	 * Creates the DocDC section to the XML SIP file.
     * @param key
     * @param itemInputStream
     * @param writer
     * @param builder
     * @throws ArchiveServiceException
     * @throws XMLStreamException
     * @throws MetadataBuilderException
     */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void createDocDC(String key, InputStream itemInputStream, XMLStreamWriter writer, XMLMetadataBuilder builder) throws ArchiveServiceException, XMLStreamException, MetadataBuilderException {

		String jsonString = null;
		try {
			jsonString = OrtolangIndexableContentParser.parse(org.apache.commons.io.IOUtils.toString(itemInputStream, "UTF-8"));
		} catch (IOException | IndexableContentParsingException e) {
			throw new ArchiveServiceException("unable to read JSON content in order to create the DocDC element", e);
		}
		
		JsonObject json = null;
		try(JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
			json = jsonReader.readObject();
		} catch(Exception e) {
			throw new ArchiveServiceException("unable to read Item Json metadata of object " + key);
		}

 	   	builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.DocDc); //// DocDC

        writeElement(Sip.DocDcTitle, json, builder);

		JsonArray contributors = json.getJsonArray("contributors");
		String creator = null;
		if (contributors != null) {
			for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
				JsonArray roles = contributor.getJsonArray("roles");
				for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
					String roleId = role.getString("id");
					if ("author".equals(roleId)) {
						creator = person(contributor);
					}
				}
			}
		}
		
		if (creator != null) {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcCreator, creator);
		}
		else {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcCreator, Sip.NRValue);
		}
		if (json.containsKey("keywords")) {
			writeElement("keywords", json, Sip.DocDcSubject, builder);
		} else {
			XmlDumpAttributes attrs = new XmlDumpAttributes();
	        attrs.put("language", "fra");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcSubject, attrs, Sip.NRValue);
		}
        writeElement(Sip.DocDcDescription, json, builder);

		JsonArray producers = json.getJsonArray("producers");
		if (producers != null) {
			for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
				if (producer.containsKey("fullname")) {
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcPublisher, producer.getString("fullname"));
				}
			}
		} else {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcPublisher, Sip.NRValue);
		}
		if (contributors != null) {
			for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
				JsonArray roles = contributor.getJsonArray("roles");
				for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
					String roleId = role.getString("id");
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcContributor, person(contributor) + " (" + roleId + ")");
				}
			}
		}
		JsonString creationDate = json.getJsonString("originDate");
		if (creationDate != null) {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcDate, creationDate.getString());
		} else {
			JsonString publicationDate = json.getJsonString("publicationDate");
			if (publicationDate != null) {
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcDate, publicationDate.getString());
			}
		}

		XmlDumpAttributes attrEng = new XmlDumpAttributes();
		attrEng.put("language", "eng");
		XmlDumpAttributes attrFra = new XmlDumpAttributes();
		attrFra.put("language", "fra");
		JsonString resourceType = json.getJsonString("type");
		if (resourceType != null) {
			switch(resourceType.getString()) {
				case ORTOLANG_RESOURCE_TYPE_CORPORA:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcType, attrEng, CMDI_RESOURCE_CLASS_CORPUS); break;
				case ORTOLANG_RESOURCE_TYPE_LEXICON:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcType, attrEng, OLAC_LINGUISTIC_TYPES.get(1)); break;
				case ORTOLANG_RESOURCE_TYPE_TERMINOLOGY:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcType, attrEng, CMDI_RESOURCE_CLASS_TERMINOLOGY); break;
				case ORTOLANG_RESOURCE_TYPE_TOOL:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcType, attrEng, CMDI_RESOURCE_CLASS_TOOL_SERVICE); break;
				case CMDI_RESOURCE_CLASS_WEBSITE:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcType, attrEng, CMDI_RESOURCE_CLASS_WEBSITE); break;
			}
		}
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcFormat, attrFra, Sip.NRValue);
		// TODO find the iso639_3 for the language
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcLanguage, "fra");

		JsonObject statusOfUse = json.getJsonObject("statusOfUse");
		if (statusOfUse != null) {
			String idStatusOfUse = statusOfUse.getString("id");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcRights, attrEng, idStatusOfUse);

			JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
			for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
				writeMultilingualElement(Sip.DocDcRights, label, builder);
			}
		}
		JsonArray conditionsOfUse = json.getJsonArray("conditionsOfUse");
		if (conditionsOfUse != null) {
			for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
				writeMultilingualElement(Sip.DocDcRights, label, builder);
			}
		}
		JsonObject license = json.getJsonObject("license");
		if (license != null && license.containsKey("label")) {
			XmlDumpAttributes attrs = new XmlDumpAttributes();
	        attrs.put("language", "fra");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocDcRights, attrs, XMLDocument.removeHTMLTag(license.getString("label")));
		}
		
        writer.writeEndElement(); //// DocDC
    }

    /**
	 * Creates the DocMeta of the XML SIP file.
     * @param alias
     * @param writer
     * @param builder
     * @throws XMLStreamException
     * @throws MetadataBuilderException
     */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void createDocMeta(String alias, XMLStreamWriter writer, XMLMetadataBuilder builder) throws XMLStreamException, MetadataBuilderException {

 	   builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.DocMeta); //// DocMeta

       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaIdentifiantDocProducteur, alias);
       builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaEvaluation);
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaDUA, "P15Y");
       
       XmlDumpAttributes attrs = new XmlDumpAttributes();
       attrs.put("language", "fra");
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaTraitement, attrs, "conservation définitive");
       String currentDate = DateUtils.getCurrentDate();
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaDateDebut, currentDate);
       builder.writeEndElement(); // evaluation
       
       builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaCommunicabilite);
       
       XmlDumpAttributes attrsCode = new XmlDumpAttributes();
       attrsCode.put("type", "SEDA");
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaCode, attrsCode, "AR038");
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaDateDebut, currentDate);
       builder.writeEndElement(); // communicabilite
       
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaServiceVersant, "ORTOLANG");
       XmlDumpAttributes attrsPlanClassement = new XmlDumpAttributes();
       attrsPlanClassement.put("language", "fra");
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.DocMetaPlanClassement, attrsPlanClassement, "ortolang");
 	   
 	   writer.writeEndElement(); //// DocMeta

    }

	
    /**
	 * Creates the section FichMeta to the XML SIP file.
     * @param archiveList
     * @param writer
     * @param builder
     * @param schema
     * @throws XMLStreamException
     * @throws MetadataBuilderException
     */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void createFichMeta(List<ArchiveEntry> archiveList, XMLStreamWriter writer, XMLMetadataBuilder builder, String schema) throws XMLStreamException, MetadataBuilderException {
 	   for(ArchiveEntry entry : archiveList) {
 		  builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.FichMeta); //// FichMeta
 		  
 		  // Removes the first '/' from the absolute path in the workspace ortolang
 		  if (entry.getEncoding() != null && !entry.getEncoding().equals(Sip.NAValue)) {
 			  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaEncodage, entry.getEncoding().toUpperCase());
 		  }
 		  if (entry.getFormat().equals(OrtolangXMLParser.XMLType.TEI.name()) && entry.getPath().startsWith("/DESC")) {
			 builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaFormatFichier, "XML");
		  } else {
			  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaFormatFichier, entry.getFormat());
		  }
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaNomFichier, entry.getPath().substring(1));
 		  XmlDumpAttributes attrs = new XmlDumpAttributes();
	      attrs.put("type", Sip.FichMetaMD5);
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaEmpreinteOri, attrs, entry.getMd5sum());
 		  if (entry.getFormat().equals(OrtolangXMLParser.XMLType.TEI.name()) && !entry.getPath().startsWith("/DESC") && schema != null) {
 			 builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaStructureFichier, schema);
 		  }
 		  
 		  writer.writeEndElement(); //// FichMeta
 	   }
    }
    
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private static void writeElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeElement(elementName, meta, elementName, builder);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private static void writeElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
				if (elm.containsKey("lang") && elm.containsKey("value")) {
					writeMultilingualElement(tagName, elm, builder);
				} else {
					if (elm.containsKey("value")) {
						builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
					}
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private static void writeMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		//TODO iso639_3
		if (multilingualObject.getString("lang").matches(iso639_2pattern)) {
//			attrs.put("language", multilingualObject.getString("lang"));
			attrs.put("language", "fra");
		}
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}

	
	/** 
	 * Builds a list of archive entry based on the content of a workspace.
	 * @param wskey a workspace key
	 * @param snapshot a snapshot workspace or null to get the head collection
	 * @throws ArchiveServiceException
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ArchiveEntry> buildWorkspaceArchiveList(String wskey, String snapshot) throws ArchiveServiceException {
		List<ArchiveEntry> imported = new ArrayList<>();
		try {
			PathBuilder pbuilder = PathBuilder.fromPath("/");
			OrtolangObjectIdentifier identifier = registry.lookup(wskey);
			Workspace workspace = em.find(Workspace.class, identifier.getId());
			if (workspace == null) {
                throw new ArchiveServiceException(
                        "unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
			String root = null;
			if ( snapshot == null ) {
				root = workspace.getHead();
			} else {
				if (!workspace.containsSnapshotName(snapshot)) {
					throw new ArchiveServiceException(
							"the workspace with key: " + wskey + " does not contain a snapshot with name: " + snapshot);
				}
				root = workspace.findSnapshotByName(snapshot).getKey();
			}

			buildArchiveList(root, pbuilder, imported);
			
		} catch (InvalidPathException e) {
			throw new ArchiveServiceException("invalid path '/' to workspace " + wskey, e);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new ArchiveServiceException("unable to get object from registry with key " + wskey, e);
		}
		return imported;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private void buildArchiveList(String key, PathBuilder path, List<ArchiveEntry> imported) throws ArchiveServiceException {
		OrtolangObjectIdentifier identifier;
    	OrtolangObject object;
		try {
			identifier = registry.lookup(key);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new ArchiveServiceException("unable to get object from registry with key " + key, e);
		}
        String type = identifier.getType();
        switch (type) {

            case Collection.OBJECT_TYPE:
            LOGGER.log(Level.FINE, "Collection path is {0}", path);
            object = em.find(Collection.class, identifier.getId());
            Set<CollectionElement> elements = ((Collection) object).getElements();
            for (CollectionElement element : elements) {
                PathBuilder pelement;
				try {
                    pelement = path.clone().path(element.getName());
                    LOGGER.log(Level.FINE, "Go through collection {0}", pelement);
                    buildArchiveList(element.getKey(), pelement, imported);
				} catch (InvalidPathException e) {
					throw new ArchiveServiceException("invalid path to object " + path.build(), e);
				}
            }
            break;

            case DataObject.OBJECT_TYPE:
            	object = em.find(DataObject.class, identifier.getId());
            	Validator validator = getArchivableMetadata((DataObject) object);
				if ( validator == null) {
					throw new ArchiveServiceException("unable to get archivable metadata from data object " + object);
				} else if (Boolean.TRUE.equals(validator.getArchivable())) {
					imported.add( ArchiveEntry.newArchiveEntry(key, ((DataObject) object).getStream(), path.build(), 
						((DataObject) object).getSize(), validator ));
            	}
            break;
            default:
                LOGGER.log(Level.WARNING, "unexpected type during importation to SIP : objet type not implemented : {0}", key);
            break;

		}
	}

	/**
	 * Adds an entry to the archive.
	 * @param entry an archive entry
	 * @param archiveOutput the archive output stream
	 * @throws ArchiveServiceException
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void addEntryToArchive(ArchiveEntry entry, ArchiveOutputStream archiveOutput) throws ArchiveServiceException {
		try (InputStream input = binarystore.get((entry.getStream()))) {
			addInputstreamToArchive(input, entry, archiveOutput);
		} catch (IOException | DataNotFoundException | BinaryStoreServiceException | ArchiveServiceException e) {
			throw new ArchiveServiceException("unable to copy input stream of dataobject " + entry.getKey(), e);
		}
	}

	/**
	 * Adds the content of a file to an archive.
	 * @param input the inputstream to the content
	 * @param entry informations about the file and its content
	 * @param archiveOutput the archive targeted
	 * @throws ArchiveServiceException
	 * @throws IOException this exception is thrown only if the archive entry cannot be closed
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private void addInputstreamToArchive(InputStream input, ArchiveEntry entry, ArchiveOutputStream archiveOutput) throws ArchiveServiceException, IOException {
		try {
			PathBuilder pbuilder = PathBuilder.fromPath(entry.getPath());
			LOGGER.log(Level.FINE, "Copying file to tar at path {0}}", pbuilder.build());
			// Removes the first '/' (issue ortolang/ortolang-diffusion#13)
			TarArchiveEntry tarEntry = new TarArchiveEntry(pbuilder.build().substring(1));
			tarEntry.setSize(entry.getSize());
			archiveOutput.putArchiveEntry(tarEntry);
			IOUtils.copy(input, archiveOutput);
		} catch (IOException | InvalidPathException e) {
			throw new ArchiveServiceException("unable to copy input stream of dataobject " + entry.getKey(), e);
		} finally {
			archiveOutput.closeArchiveEntry();
		}
	} 

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private Validator getArchivableMetadata(DataObject object) {
    	Validator validator = null;
        MetadataElement mde = object.findMetadataByName(MetadataFormat.FACILE_VALIDATOR);
        if (mde != null) {
        	try {
				MetadataObject meta = core.systemReadMetadataObject(mde.getKey());
				ObjectMapper mapper = new ObjectMapper();
				validator = mapper.readValue(binarystore.getFile(meta.getStream()), Validator.class);
			} catch (AccessDeniedException | CoreServiceException | KeyNotFoundException | IOException | BinaryStoreServiceException | DataNotFoundException e) {
				LOGGER.log(Level.WARNING, "unexpected error when reading metadata object", e);
			}
        }
    	return validator;
    }

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private Validator validateFacile(DataObject dataObject) throws BinaryStoreServiceException, DataNotFoundException, CheckArchivableException {
		// Validation via FACILE
		String hash = dataObject.getStream();
		LOGGER.log(Level.FINE, "Checking archivable for data object {0}", dataObject.getKey());
		File content = binarystore.getFile(hash);
		InputStream input = binarystore.get(hash);
		Validator validator = null;
		XMLMetadata xmlMetadata = getXMLMetadata(dataObject);
		if (xmlMetadata != null && xmlMetadata.getFormat().contentEquals(OrtolangXMLParser.XMLType.TEI.name())) {
			validator = new Validator();
			validator.setFormat(xmlMetadata.getFormat());
			validator.setValid(true);
			validator.setWellFormed(true);
			validator.setArchivable(true); // TODO check with the schema
			validator.setEncoding(xmlMetadata.getEncoding());
			validator.setVersion(xmlMetadata.getVersion());
			validator.setFileName(dataObject.getName());
			validator.setSize(dataObject.getSize());
			try {
				HashedFilterInputStream hashInputStream = factory.getHashedFilterInputStream(input);
				byte[] buffer = new byte[10240];
				while (hashInputStream.read(buffer) >= 0) {
				}
				String hashCode = hashInputStream.getHash();
				validator.setMd5sum(hashCode);
			} catch (NoSuchAlgorithmException | IOException e) {
				validator.setMessage(e.getMessage());
				LOGGER.log(Level.WARNING, "Cant generate md5 for the data object", e);
			}
		} else {
			validator = facile.checkArchivableFile(content, dataObject.getName());
		}

		return validator;
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private String writeJson(Validator validator) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(validator);
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private XMLMetadata getXMLMetadata(DataObject dataObject) {
		try {
			MetadataElement xmlMd = dataObject.findMetadataByName(MetadataFormat.XML);
			if (xmlMd != null) {
				OrtolangObjectIdentifier itemMetadataIdentifier;
					itemMetadataIdentifier = registry.lookup(xmlMd.getKey());
				MetadataObject metadataObject = em.find(MetadataObject.class, itemMetadataIdentifier.getId());
				return extractXMLMetadata(metadataObject);
			}
		} catch (RegistryServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.WARNING, "Enable to check the format of the data object {0}", dataObject.getKey());
		}
		return null;
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private XMLMetadata extractXMLMetadata(MetadataObject metadataObject) {
		XMLMetadata md = new XMLMetadata();
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> xmlContent;
		try {
			xmlContent = mapper.readValue(binarystore.getFile(metadataObject.getStream()), new TypeReference<Map<String, Object>>(){});
			if (xmlContent.containsKey(OrtolangXMLParser.XML_TYPE_KEY)) {
				md.setFormat((String) xmlContent.get(OrtolangXMLParser.XML_TYPE_KEY));
			}
			if (xmlContent.containsKey(Metadata.CONTENT_ENCODING)) {
				md.setEncoding((String) xmlContent.get(Metadata.CONTENT_ENCODING));
			}
			if (xmlContent.containsKey("XML-Version")) {
				md.setVersion((String) xmlContent.get("XML-Version"));
			}
		} catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.WARNING, "Enable to extract format from metadata object", e);
		}
		return md;
	}
	
	class XMLMetadata {
		private String format;
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		public String getEncoding() {
			return encoding;
		}
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		private String encoding;
		private String version;
		
	}

	
}