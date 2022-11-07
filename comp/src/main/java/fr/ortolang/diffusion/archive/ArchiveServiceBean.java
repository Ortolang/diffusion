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
import java.util.Collections;
import java.util.HashMap;
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
        LOGGER.log(Level.INFO, "Initializing archive service with base directory : {0}" , base.toString());
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
	 * Creates a SIP archive ready to send to the PAC platform.
	 * @param key a workspace key
	 * @param schema a schema for TEI XML files
	 */
	@Override
	public void createSIP(String key, String schema) throws ArchiveServiceException {
		sendMessage(key, ArchiveService.CREATE_SIP_ACTION, schema);
    }

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void sendMessage(String key, String action) throws ArchiveServiceException {
		sendMessage(key, action, null);
	}

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void sendMessage(String key, String action, String schema) throws ArchiveServiceException {
        try {
            Message message = context.createMessage();
			message.setStringProperty("action", action);
            message.setStringProperty("key", key);
			if ( schema != null ) {
				message.setStringProperty("schema", schema);
			}
            context.createProducer().send(archiveQueue, message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to send archive message", e);
            throw new ArchiveServiceException("unable to send archive message", e);
        }
    }

	/**
	 * Creates a SIP archive ready to send to the PAC platform.
	 * @param key a workspace key
	 * @param schema a schema for TEI XML files
	 */
	public void createSIPTar(String key, String schema) throws ArchiveServiceException {
		LOGGER.log(Level.FINE, "Create SIP tar archive for object {0}", key);
        Map<String, Validator> imported = new HashMap<>();
		
		try (TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(Files.newOutputStream(base.resolve(key + ".tar")))) {
			importPathToSIP(key, tarOutput, imported);
			importSIPXml(key, tarOutput, imported, schema);
			tarOutput.finish();
		} catch (IOException | ArchiveServiceException | AccessDeniedException | CoreServiceException | KeyNotFoundException | BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error during creating to SIP Tar", e);
			throw new ArchiveServiceException("unable to create the Tar " + base.resolve(key + ".tar"), e);
		}
    }

	/**
	 * Imports the metadata file SIP.xml to the Tar.
	 * @param wsKey workspace key
	 * @param tarOutput the tar archive
	 * @param imported a map containing all the imported files
	 * @param schema the path to the XML schema
	 * @throws KeyNotFoundException
	 * @throws CoreServiceException
	 * @throws ArchiveServiceException
	 * @throws AccessDeniedException
	 * @throws DataNotFoundException
	 * @throws BinaryStoreServiceException
	 */
	private void importSIPXml(String wsKey, TarArchiveOutputStream tarOutput, Map<String, Validator> imported, String schema) throws CoreServiceException, KeyNotFoundException, ArchiveServiceException, AccessDeniedException, BinaryStoreServiceException, DataNotFoundException {
		Workspace workspace = core.systemReadWorkspace(wsKey);
    	String root = workspace.getHead();
		MetadataElement itemMetadata = core.systemReadCollection(root).findMetadataByName(MetadataFormat.ITEM);
    	if (itemMetadata == null) {
    		throw new ArchiveServiceException("No meta type ITEM to the root collection " + root);
    	}

		MetadataObject md = core.systemReadMetadataObject(itemMetadata.getKey());

		if (md == null) {
    		throw new ArchiveServiceException("Unable to read MetadataObject with key " + itemMetadata.getKey());
    	}

		java.nio.file.Path xmlSipFile = null;
		try {
			xmlSipFile = Files.createTempFile("sip", ".xml");
		} catch (IOException eCreateTempFile) {
			LOGGER.log(Level.WARNING, "unexpected error during the creation of the xml sip file", eCreateTempFile);
		}

		if (xmlSipFile != null) {
			try (OutputStream os = Files.newOutputStream(xmlSipFile)) {
				XMLStreamWriter writer = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(os));
	
				XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
				   
				XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
				namespaces.put(SIP_NAMESPACE_PREFIX, new XmlDumpNamespace(SIP_NAMESPACE_URI, SIP_NAMESPACE_SCHEMA_LOCATION));
				namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
				builder.setNamespaces(namespaces);
				builder.writeStartDocument(SIP_NAMESPACE_PREFIX, Sip.Pac, null);
	
				createDocDC(wsKey, binarystore.get(md.getStream()), writer, builder);
				createDocMeta(workspace.getAlias(), writer, builder);
				createFichMeta(imported, writer, builder, schema);
	
				writer.writeEndElement(); // end of Pac
				writer.writeEndDocument();
				writer.flush();
				writer.close();
				
				// Validate the sip.xml generated by this class
				validateSipXml(xmlSipFile);
			} catch (IOException | XMLStreamException | MetadataBuilderException e) {
				throw new ArchiveServiceException("unable to create SIP XML", e);
			}
	
			TarArchiveEntry tarEntry = new TarArchiveEntry(SIP_XML_FILE);
			try {
				tarEntry.setSize(Files.size(xmlSipFile));
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "unexpected error during importation of the xml sip file to the Tar", e);
			}
			try {
				tarOutput.putArchiveEntry(tarEntry);
				IOUtils.copy(Files.newInputStream(xmlSipFile), tarOutput);
			} catch(IOException eTarEntry) {
				LOGGER.log(Level.SEVERE, "unexpected error during importation of the xml sip file to the Tar", eTarEntry);
				throw new ArchiveServiceException("unable to copy input stream of file : " + xmlSipFile, eTarEntry);
			} finally {
				try {
					tarOutput.closeArchiveEntry();
				} catch (IOException eCloseTarEntry) {
					LOGGER.log(Level.SEVERE, "unexpected error during importation of the xml sip to the Tar", eCloseTarEntry);
					throw new ArchiveServiceException("unable to close archive entry for path " + xmlSipFile, eCloseTarEntry);
				}
			}
	
			try {
				Files.delete(xmlSipFile);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "unexpected error during importation of the xml sip file to the Tar", e);
			}
		}
	}

	private void importPathToSIP(String key, TarArchiveOutputStream tarOutput, Map<String, Validator> imported) throws ArchiveServiceException {
		try {
			PathBuilder pbuilder = PathBuilder.fromPath("/");
			importToSIP(key, pbuilder, tarOutput, imported);
		} catch (InvalidPathException e) {
			LOGGER.log(Level.WARNING, "invalid path '/' to object {O}", key);
		}
	}

	/**
	 * Validates the XML SIP file.
	 * @param path path to the XML SIP file
	 */
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

    private void createFichMeta(Map<String, Validator> imported, XMLStreamWriter writer, XMLMetadataBuilder builder, String schema) throws XMLStreamException, MetadataBuilderException {
 	   for(Map.Entry<String, Validator> entry : imported.entrySet()) {
 		  builder.writeStartElement(SIP_NAMESPACE_PREFIX, Sip.FichMeta); //// FichMeta
 		  
 		  // Removes the first '/' from the absolute path in the workspace ortolang
 		  if (entry.getValue().getEncoding() != null && !entry.getValue().getEncoding().equals(Sip.NAValue)) {
 			  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaEncodage, entry.getValue().getEncoding().toUpperCase());
 		  }
 		  if (entry.getValue().getFormat().equals(OrtolangXMLParser.XMLType.TEI.name()) && entry.getKey().startsWith("/DESC")) {
			 builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaFormatFichier, "XML");
		  } else {
			  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaFormatFichier, entry.getValue().getFormat());
		  }
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaNomFichier, entry.getKey().substring(1));
 		  XmlDumpAttributes attrs = new XmlDumpAttributes();
	      attrs.put("type", Sip.FichMetaMD5);
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaEmpreinteOri, attrs, entry.getValue().getMd5sum());
 		  if (entry.getValue().getFormat().equals(OrtolangXMLParser.XMLType.TEI.name()) && !entry.getKey().startsWith("/DESC") && schema != null) {
 			 builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, Sip.FichMetaStructureFichier, schema);
 		  }
 		  
 		  writer.writeEndElement(); //// FichMeta
 	   }
    }
    
    
    
	public static void writeElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeElement(elementName, meta, elementName, builder);
	}
	
	public static void writeElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
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

	public static void writeMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		//TODO iso639_3
		if (multilingualObject.getString("lang").matches(iso639_2pattern)) {
//			attrs.put("language", multilingualObject.getString("lang"));
			attrs.put("language", "fra");
		}
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}

    /**
     * Converts ortolang object to the File system.
     * @param key a ortolang key
     * @param path path into the workspace
     * @param basepath path to the filesystem
     * @throws ArchiveServiceException
     */
    private void importToSIP(String key, PathBuilder path, ArchiveOutputStream tarOutput, Map<String,Validator> imported ) throws ArchiveServiceException {
    	OrtolangObjectIdentifier identifier;
    	OrtolangObject object;
		try {
			identifier = registry.lookup(key);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error during importation to SIP", e);
			throw new ArchiveServiceException("unable to get object from registry with key " + key, e);
		}
        String type = identifier.getType();
        switch (type) {
            case Workspace.OBJECT_TYPE:
			object = em.find(Workspace.class, identifier.getId());
            String head = ((Workspace) object).getHead();
            LOGGER.log(Level.FINE, "Workspace head is {0}", head);
            importToSIP(head, path, tarOutput, imported);
            break;

            case Collection.OBJECT_TYPE:
            LOGGER.log(Level.FINE, "Collection path is {0}", path);
            object = em.find(Collection.class, identifier.getId());
            Set<CollectionElement> elements = ((Collection) object).getElements();
            for (CollectionElement element : elements) {
                PathBuilder pelement;
				try {
                    pelement = path.clone().path(element.getName());
                    LOGGER.log(Level.FINE, "Go through collection {0}", pelement);
                    importToSIP(element.getKey(), pelement, tarOutput, imported);
				} catch (InvalidPathException e) {
					throw new ArchiveServiceException("invalid path to object " + path.build(), e);
				}
            }
            break;

            case DataObject.OBJECT_TYPE:
            	object = em.find(DataObject.class, identifier.getId());
            	Validator validator = getArchivableMetadata((DataObject) object);
				if ( validator == null) {
					LOGGER.log(Level.WARNING, "unable to get archivable metadata from data object {0}", object);
				} else if (Boolean.TRUE.equals(validator.getArchivable())) {
            		try (InputStream input = binarystore.get(((DataObject) object).getStream())) {
						LOGGER.log(Level.FINE, "Copying file to tar at path {0}}", path.build());
						// Removes the first '/' (issue ortolang/ortolang-diffusion#13)
						TarArchiveEntry tarEntry = new TarArchiveEntry(path.build().substring(1));
						tarEntry.setSize(((DataObject) object).getSize());
						try {
							tarOutput.putArchiveEntry(tarEntry);
							IOUtils.copy(input, tarOutput);
						} catch(IOException eTarEntry) {
							LOGGER.log(Level.SEVERE, "unexpected error during importation to the Tar", eTarEntry);
            				throw new ArchiveServiceException("unable to copy input stream of path: " + path.build(), eTarEntry);
						} finally {
							try {
								tarOutput.closeArchiveEntry();
							} catch (IOException eCloseTarEntry) {
								LOGGER.log(Level.SEVERE, "unexpected error during importation to the Tar", eCloseTarEntry);
            					throw new ArchiveServiceException("unable to close archive entry for path " + path.build(), eCloseTarEntry);
							}
						}
            			imported.put(path.build(), validator);
            		} catch (IOException | DataNotFoundException | BinaryStoreServiceException e) {
            			LOGGER.log(Level.SEVERE, "unexpected error during importation to SIP", e);
            			throw new ArchiveServiceException("unable to copy input stream to path: " + path.build(), e);
            		}
            	}
            break;
            default:
                LOGGER.log(Level.WARNING, "unexpected type during importation to SIP : objet type not implemented : {0}", key);
            break;
        }
    }

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

	@Override
	public void validateDataobject(String key) throws ArchiveServiceException {
		OrtolangObjectIdentifier identifier;
		try {
			identifier = registry.lookup(key);
			
		} catch (RegistryServiceException | KeyNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error during validation of dataobject " + key, e);
			throw new ArchiveServiceException("unable to get object from registry with key " + key, e);
		}
		if (!identifier.getService().equals(core.getServiceName())
			|| !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
			throw new ArchiveServiceException("unable to validate other than a DataObject");
		}
		DataObject dataObject = (DataObject) em.find(DataObject.class, identifier.getId());
		dataObject.setKey(key);
		Validator validator = null;
		try {
			validator = validateFacile(dataObject);
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
			LOGGER.log(Level.SEVERE, "unexpected error during validation of dataobject " + key, e);
			throw new ArchiveServiceException("unable to validate via Facile the dataobject " + key, e);
		}
		
	}

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
	
	private String writeJson(Validator validator) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(validator);
	}
	
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
			LOGGER.log(Level.WARNING, "Enable to check the format of the data object {0}", new Object[] {dataObject.getKey(), e});
		}
		return null;
	}
	
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