package fr.ortolang.diffusion.archive;

import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_CORPUS;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_TERMINOLOGY;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_TOOL_SERVICE;
import static fr.ortolang.diffusion.oai.format.Constant.CMDI_RESOURCE_CLASS_WEBSITE;
import static fr.ortolang.diffusion.oai.format.Constant.DC_ELEMENTS;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;
import fr.ortolang.diffusion.archive.facile.entity.Validator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.util.XmlUtils;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

import org.activiti.bpmn.converter.IndentingXMLStreamWriter;
import org.apache.commons.io.FileUtils;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    
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
        LOGGER.log(Level.INFO, "Initializing archive service with base directory : {0}", base);
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
        sendMessage(key);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void sendMessage(String key) throws ArchiveServiceException {
        try {
            Message message = context.createMessage();
            message.setStringProperty("key", key);
            context.createProducer().send(archiveQueue, message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to send archive message", e);
            throw new ArchiveServiceException("unable to send archive message", e);
        }
    }

	@Override
	public void createSIP(String key) throws ArchiveServiceException {
		Map<String, Validator> imported = createSIPDirectories(key);
		try {
			createSIPXML(key, imported);
		} catch (RegistryServiceException | KeyNotFoundException | CoreServiceException | BinaryStoreServiceException
				| DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unable create sip.xml", e);
            throw new ArchiveServiceException("unable to create sip.xml", e);
		}
//		validateSipXml(key);
    }
    
    /**
     * Creates SIP sub directories (https://www.cines.fr/archivage/des-expertises/les-metadonnees/bordereaux-de-versement/).
     * @param key workspace key
     */
    private Map<String, Validator> createSIPDirectories(String key) throws ArchiveServiceException {
        LOGGER.log(Level.FINE, "Create SIP directories for object {0}", key);
        Map<String, Validator> imported = new HashMap<String, Validator>();
		try {
            PathBuilder pbuilder = PathBuilder.fromPath("/");
            importToSIP(key, pbuilder, base, imported);
		} catch (InvalidPathException e) {
            throw new ArchiveServiceException("invalid path '/' to object " + key, e);
		}
		return imported;
    }

    /**
     * Creates a SIP XML.
     * @param key workspace key
     * @throws KeyNotFoundException 
     * @throws RegistryServiceException 
     * @throws CoreServiceException 
     * @throws DataNotFoundException 
     * @throws BinaryStoreServiceException 
     */
    private void createSIPXML(String key, Map<String, Validator> imported) throws ArchiveServiceException, RegistryServiceException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException, DataNotFoundException {
    	Workspace workspace = core.systemReadWorkspace(key);
    	String snapshot = core.findWorkspaceLatestPublishedSnapshot(key);
    	if (snapshot == null) {
			LOGGER.log(Level.WARNING, "finds no published snapshot for workspace {0}", key);
			throw new ArchiveServiceException("unable to find published snapshot for workspace " + key);
		}
    	String root = workspace.findSnapshotByName(snapshot).getKey();
		String item = null;
		try {
			List<OrtolangIndexableContent> indexableContents = core.getIndexableContent(root);
			if (indexableContents.size() > 0) {
				item = indexableContents.get(0).getContent();
			}
		} catch (KeyNotFoundException | RegistryServiceException | IndexingServiceException | OrtolangException e1) {
			LOGGER.log(Level.SEVERE, "unable to get json content from root collection {0}", root);
			throw new ArchiveServiceException("unable to get json content from root collection " + root, e1);
		}

    	if (item == null) {
    		throw new ArchiveServiceException("No meta type ITEM to the root collection " + root);
    	}
    	
    	try (OutputStream os = Files.newOutputStream(base.resolve(key).resolve(SIP_XML_FILE))) {

    		XMLOutputFactory factory = XMLOutputFactory.newInstance();
    		XMLStreamWriter writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(os));

    		XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
    	 	  
    		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
    		namespaces.put(SIP_NAMESPACE_PREFIX, new XmlDumpNamespace(SIP_NAMESPACE_URI, SIP_NAMESPACE_SCHEMA_LOCATION));
    		namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
    		builder.setNamespaces(namespaces);
    		builder.writeStartDocument(SIP_NAMESPACE_PREFIX, "pac", null);

    		createDocDC(key, item, writer, builder);
    		createDocMeta(workspace.getAlias(), writer, builder);
    		createFichMeta(imported, writer, builder);

    		writer.writeEndElement();
    		writer.writeEndDocument();
    		writer.flush();
    		writer.close();
    		
    		// Validate the sip.xml generated by this class
    		validateSipXml(key);
    	} catch (IOException | XMLStreamException | MetadataBuilderException e) {
    		throw new ArchiveServiceException("unable to create SIP XML", e);
    	}
    }
    
    private void validateSipXml(String key) throws ArchiveServiceException {
		try {
			String result = FileUtils.readFileToString(base.resolve(key).resolve(SIP_XML_FILE).toFile(), StandardCharsets.UTF_8);
	    	if (!result.toString().isEmpty()) {
				XmlUtils.validateXml(result.toString());
			} else {
				throw new ArchiveServiceException("unable to validate xml cause sip.xml file is empty");
			}
		} catch (IOException e) {
			throw new ArchiveServiceException("unable to create SIP XML", e);
		} catch (SAXException e) {
			throw new ArchiveServiceException("sip.xml is not valid", e);
		}
    }

    private void createDocDC(String key, String item, XMLStreamWriter writer, XMLMetadataBuilder builder) throws ArchiveServiceException, XMLStreamException, MetadataBuilderException {

    	StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
    	JsonObject json = jsonReader.readObject();

 	   	builder.writeStartElement(SIP_NAMESPACE_PREFIX, "DocDC"); //// DocDC

        writeElement("title", json, builder);

		JsonArray contributors = json.getJsonArray("contributors");
		if (contributors != null) {
			for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
				JsonArray roles = contributor.getJsonArray("roles");
				for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
					String roleId = role.getString("id");
					if ("author".equals(roleId)) {
						builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "creator", person(contributor));
					} else {
						builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "creator", "Non renseigné");
					}
				}
			}
		} else {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "creator", "Non renseigné");
		}
		if (json.containsKey("keywords")) {
			writeElement("keywords", json, "subject", builder);
		} else {
			XmlDumpAttributes attrs = new XmlDumpAttributes();
	        attrs.put("language", "fra");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "subject", attrs, "Non renseigné");
		}
        writeElement("description", json, builder);

		JsonArray producers = json.getJsonArray("producers");
		if (producers != null) {
			for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
				if (producer.containsKey("fullname")) {
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
				}
			}
		} else {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "publisher", "Non renseigné");
		}
		if (contributors != null) {
			for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
				JsonArray roles = contributor.getJsonArray("roles");
				for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
					String roleId = role.getString("id");
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "contributor", person(contributor) + " (" + roleId + ")");
				}
			}
		}
		JsonString creationDate = json.getJsonString("originDate");
		if (creationDate != null) {
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "date", creationDate.getString());
		} else {
			JsonString publicationDate = json.getJsonString("publicationDate");
			if (publicationDate != null) {
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "date", publicationDate.getString());
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
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrEng, CMDI_RESOURCE_CLASS_CORPUS); break;
				case ORTOLANG_RESOURCE_TYPE_LEXICON:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrEng, OLAC_LINGUISTIC_TYPES.get(1)); break;
				case ORTOLANG_RESOURCE_TYPE_TERMINOLOGY:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrEng, CMDI_RESOURCE_CLASS_TERMINOLOGY); break;
				case ORTOLANG_RESOURCE_TYPE_TOOL:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrEng, CMDI_RESOURCE_CLASS_TOOL_SERVICE); break;
				case CMDI_RESOURCE_CLASS_WEBSITE:
					builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrEng, CMDI_RESOURCE_CLASS_WEBSITE); break;
			}
		}
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "format", attrFra, "Non renseigné");
		// TODO find the iso639_3 for the language
		builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "language", "fra");

		JsonObject statusOfUse = json.getJsonObject("statusOfUse");
		if (statusOfUse != null) {
			String idStatusOfUse = statusOfUse.getString("id");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "rights", attrEng, idStatusOfUse);

			JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
			for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
				writeMultilingualElement("rights", label, builder);
			}
		}
		JsonArray conditionsOfUse = json.getJsonArray("conditionsOfUse");
		if (conditionsOfUse != null) {
			for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
				writeMultilingualElement("rights", label, builder);
			}
		}
		JsonObject license = json.getJsonObject("license");
		if (license != null && license.containsKey("label")) {
			XmlDumpAttributes attrs = new XmlDumpAttributes();
	        attrs.put("language", "fra");
			builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "rights", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
		}
		
        writer.writeEndElement(); //// DocDC
        reader.close();
    }

    private void createDocMeta(String alias, XMLStreamWriter writer, XMLMetadataBuilder builder) throws XMLStreamException, MetadataBuilderException {

 	   builder.writeStartElement(SIP_NAMESPACE_PREFIX, "DocMeta"); //// DocMeta

       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "identifiantDocProducteur", alias);
       builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "serviceVersant", "ORTOLANG");
 	   
 	   writer.writeEndElement(); //// DocMeta

    }

    private void createFichMeta(Map<String, Validator> imported, XMLStreamWriter writer, XMLMetadataBuilder builder) throws XMLStreamException, MetadataBuilderException {
 	   for(Map.Entry<String, Validator> entry : imported.entrySet()) {
 		  builder.writeStartElement(SIP_NAMESPACE_PREFIX, "FichMeta"); //// FichMeta
 		  
 		  // Removes the first '/' from the absolute path in the workspace ortolang
 		  if (!entry.getValue().getEncoding().equals("NA")) {
 			  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "encodage", entry.getValue().getEncoding());
 		  }
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "formatFichier", entry.getValue().getFormat());
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "nomFichier", entry.getKey().substring(1));
 		  XmlDumpAttributes attrs = new XmlDumpAttributes();
	      attrs.put("type", "MD5");
 		  builder.writeStartEndElement(SIP_NAMESPACE_PREFIX, "empreinteOri", attrs, entry.getValue().getMd5sum());
 		 
 		  writer.writeEndElement(); //// DocMeta
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
    private void importToSIP(String key, PathBuilder path, Path basepath, Map<String,Validator> imported) throws ArchiveServiceException {
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
            Path depot = base.resolve(key).resolve(DEPOT_DIRECTORY);
            LOGGER.log(Level.FINE, "Workspace path is {0}", depot);
            createDirectory(depot.resolve(DESC_DIRECTORY));
			object = em.find(Workspace.class, identifier.getId());
            String head = ((Workspace) object).getHead();
            importToSIP(head, path, depot, imported);
            break;

            case Collection.OBJECT_TYPE:
            LOGGER.log(Level.FINE, "Collection path is {0}", basepath);
            createDirectory(basepath);
            object = em.find(Collection.class, identifier.getId());
            Set<CollectionElement> elements = ((Collection) object).getElements();
            for (CollectionElement element : elements) {
                PathBuilder pelement;
				try {
                    pelement = path.clone().path(element.getName());
                    LOGGER.log(Level.FINE, "Go through collection {0}", pelement);
                    importToSIP(element.getKey(), pelement, basepath.resolve(element.getName()), imported);
				} catch (InvalidPathException e) {
					throw new ArchiveServiceException("invalid path to object " + path.build(), e);
				}
            }
            break;

            case DataObject.OBJECT_TYPE:
            	object = em.find(DataObject.class, identifier.getId());
            	Validator validator = getArchivableMetadata((DataObject) object);
            	if (validator.getArchivable()) {
            		try (InputStream input = binarystore.get(((DataObject) object).getStream())) {
            			LOGGER.log(Level.FINE, "Copying file to {0}}", basepath);
            			Files.copy(input, basepath, StandardCopyOption.REPLACE_EXISTING);
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
    
    private void createDirectory(Path path) throws ArchiveServiceException {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
			throw new ArchiveServiceException("unable to create directory", e);
		}
    }
}