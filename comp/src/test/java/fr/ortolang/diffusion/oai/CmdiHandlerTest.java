package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.oai.format.handler.CmdiHandler;
import fr.ortolang.diffusion.util.StreamUtils;
import fr.ortolang.diffusion.util.XmlUtils;

public class CmdiHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(CmdiHandlerTest.class.getName());

	@Test
	public void writeItem() {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-item.json");
		try {
			String item_json = StreamUtils.getContent(dcInputStream);

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			CmdiHandler handler = new CmdiHandler();

			// Writes metadata from JSON to XML
			handler.writeItem(item_json, builder);
			
			writer.flush();
			writer.close();
			
			XmlUtils.validateXml(result.toString());
			
			LOGGER.log(Level.INFO, "Build CMDI from json Item");
			LOGGER.log(Level.INFO, result.toString());
		} catch (IOException | MetadataHandlerException | XMLStreamException | FactoryConfigurationError | SAXException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	@Test
	public void write() {
		//TODO
	}
}
