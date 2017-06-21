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

import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.DublinCoreHandler;
import fr.ortolang.diffusion.oai.format.XMLMetadataBuilder;
import fr.ortolang.diffusion.util.StreamUtils;

public class DublinCoreHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(DublinCoreHandlerTest.class.getName());

	@Test
	public void writeItem() {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-item.json");
		try {
			String item_json = StreamUtils.getContent(dcInputStream);

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			DublinCoreHandler handler = new DublinCoreHandler();

			// Writes metadata from JSON to XML
			handler.writeItem(item_json, builder);
			
			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, "Build OAI_DC from json Item");
			LOGGER.log(Level.INFO, result.toString());
		} catch (IOException | MetadataHandlerException | XMLStreamException | FactoryConfigurationError e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	@Test
	public void write() {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-dc.json");
		try {
			String dc_json = StreamUtils.getContent(dcInputStream);

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			DublinCoreHandler handler = new DublinCoreHandler();

			// Writes metadata from JSON to XML
			handler.write(dc_json, builder);
			
			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, "Build OAI_DC from json DC");
			LOGGER.log(Level.INFO, result.toString());

			// XMLDocumentTest.checkIfPresent(oai_dc,
			// new String[] { "title", "description", "coverage", "subject",
			// "identifier" });
		} catch (IOException | MetadataHandlerException | XMLStreamException | FactoryConfigurationError e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
}
