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
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.oai.format.handler.OlacHandler;
import fr.ortolang.diffusion.util.StreamUtils;

public class OlacHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(OlacHandlerTest.class.getName());

	@Test
	public void buildFromItem() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-item.json");
		try {
			String item_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build from json item");
//			OLAC olac = OLACFactory.buildFromJson(olac_json);

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			OlacHandler handler = new OlacHandler();

			// Writes metadata from JSON to XML
			handler.writeItem(item_json, builder);
			
			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, result.toString());
			
//			checkIfPresent(olac,
//					new String[] { "title", "description", "abstract", "coverage", "subject", "identifier" });
		} catch (IOException | XMLStreamException | FactoryConfigurationError | MetadataHandlerException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void buildFromJsonOlac() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build from json olac");
//			OLAC olac = OLACFactory.buildFromJson(olac_json);

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			OlacHandler handler = new OlacHandler();

			// Writes metadata from JSON to XML
			handler.write(olac_json, builder);
			
			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, result.toString());
			
//			checkIfPresent(olac,
//					new String[] { "title", "description", "abstract", "coverage", "subject", "identifier" });
		} catch (IOException | XMLStreamException | FactoryConfigurationError | MetadataHandlerException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonDc() {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("json/sample-dc.json");
		try {
			String dc_json = StreamUtils.getContent(inputStream);
			LOGGER.log(Level.INFO, "Build from json dc");

			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);

			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			OlacHandler handler = new OlacHandler();

			// Writes metadata from JSON to XML
			handler.write(dc_json, builder);
			
			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, result.toString());

//			checkIfPresent(olac, new String[] { "title", "description", "coverage", "subject", "identifier" });
		} catch (IOException | XMLStreamException | FactoryConfigurationError | MetadataHandlerException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
