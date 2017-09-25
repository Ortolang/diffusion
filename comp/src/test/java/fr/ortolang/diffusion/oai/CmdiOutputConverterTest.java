package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.oai.format.converter.CmdiOutputConverter;
import fr.ortolang.diffusion.util.StreamUtils;
import fr.ortolang.diffusion.util.XmlUtils;

public class CmdiOutputConverterTest {

	private static final Logger LOGGER = Logger.getLogger(CmdiOutputConverterTest.class.getName());

	public void convertFromJson(String path) {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream(path);
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);

			List<String> handles = Arrays.asList("http://handle.net/111/EEE", "http://handle.net/223/EFF");
			CmdiOutputConverter converter = new CmdiOutputConverter();
			converter.setId(path);
			converter.setListHandles(handles);
			converter.convert(olac_json, MetadataFormat.OLAC, builder);

			writer.flush();
			writer.close();

			XmlUtils.validateXml(result.toString());
			
			LOGGER.log(Level.INFO, result.toString());
		} catch (IOException | XMLStreamException | FactoryConfigurationError | MetadataConverterException | SAXException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void convertFromJsonDC() {
		LOGGER.log(Level.INFO, "Building CMDI from DC");
		convertFromJson("json/sample-dc.json");
	}

	@Test
	public void convertFromJsonOLAC() {
		LOGGER.log(Level.INFO, "Building CMDI from OLAC");
		convertFromJson("json/sample-olac.json");
	}

}
