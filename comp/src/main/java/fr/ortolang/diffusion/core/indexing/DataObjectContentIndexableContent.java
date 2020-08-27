package fr.ortolang.diffusion.core.indexing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.content.ContentSearchService;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.extraction.parser.OrtolangXMLParser;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;

/**
 * An alternative of DataObjectIndexableContent which indexes the static pid (handle with version number) and the plain-text content.
 * @author cpestel
 *
 */
public class DataObjectContentIndexableContent extends MetadataSourceIndexableContent {

	private static final Logger LOGGER = Logger.getLogger(DataObjectContentIndexableContent.class.getName());

	public static final String CONTENT_FIELD = "content";
	
	public DataObjectContentIndexableContent(DataObject object) throws IndexingServiceException, OrtolangException {
		this(object, true);
	}
	public DataObjectContentIndexableContent(DataObject object, boolean withExtraction) throws IndexingServiceException, OrtolangException {
		super(object, ContentSearchService.SERVICE_NAME, DataObject.OBJECT_TYPE);

		if (withExtraction) {
	        HandleStoreService handleStore = (HandleStoreService) OrtolangServiceLocator.lookup(HandleStoreService.SERVICE_NAME, HandleStoreService.class);
	        try {
				List<String> hdls = handleStore.listHandlesForKey(object.getKey());
				content.put("pid", hdls.get(1));
			} catch (HandleStoreServiceException e) {
				LOGGER.log(Level.FINE, "Cannot get handle of data object with key [" + object.getKey() + "]", e);
			}
			
			BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
	        try {
	        	Metadata metadata = binary.parse(object.getStream());
	        	String xmlType = metadata.get(OrtolangXMLParser.XML_TYPE_KEY);
	        	// Checks if it is a TEI XML document
	        	if (xmlType != null && xmlType.equals(OrtolangXMLParser.XMLType.TEI.name())) {
	        		content.put("content", extractFromTEI(binary.get(object.getStream())));
	        	} else {
	        		String extraction = binary.extract(object.getStream());
	        		if (!extraction.trim().isEmpty()) {
	        			content.put("content", extraction);
	        		} else {
	        			throw new IndexingServiceException("Dataobject [" + object.getKey() + "] cant be indexed cause the text cant be extracted");
	        		}
	        	}
	        } catch (BinaryStoreServiceException | DataNotFoundException | TikaException | SAXException | IOException | ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException e) {
	            LOGGER.log(Level.FINE, "Cannot extract content of data object with key [" + object.getKey() + "]", e);
	        }
		}
        setContent(content);
	}
	
	private String extractFromTEI(InputStream doc) throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xml = builder.parse(doc);
        
        InputStream xslStylesheetStream = getClass().getClassLoader().getResourceAsStream("xsl/extractTei2Text.xsl");
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(xslStylesheetStream));
        
        String text = null;
        try (OutputStream outputStream = new ByteArrayOutputStream()) {
            t.transform(new DOMSource(xml), new StreamResult(outputStream));
            text = outputStream.toString();
        }
        return text;
	}
}
