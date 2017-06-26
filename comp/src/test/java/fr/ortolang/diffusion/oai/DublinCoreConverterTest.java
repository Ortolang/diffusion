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

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.oai.format.converter.DublinCoreConverter;
import fr.ortolang.diffusion.util.StreamUtils;

public class DublinCoreConverterTest {
	
	private static final Logger LOGGER = Logger.getLogger(OAI_DCTest.class.getName());
	
	public void convertFromJsonOLAC(String path) {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream(path);
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			StringWriter result = new StringWriter();
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
			
//			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			DublinCoreConverter converter = new DublinCoreConverter();
			converter.convert(olac_json, MetadataFormat.OLAC, builder);

			writer.flush();
			writer.close();

			LOGGER.log(Level.INFO, "Build OAI_DC from json OLAC");
			LOGGER.log(Level.INFO, result.toString());
		} catch (IOException | XMLStreamException | FactoryConfigurationError | MetadataConverterException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void convertFromJsonOLAC() {
		convertFromJsonOLAC("json/sample-olac.json");
//		XMLDocumentTest.checkIfPresent(oai_dc,
//		new String[] { "title", "description", "coverage", "subject", "identifier", "type" });
	}

	@Test
	public void buildFromJsonOLACRule2() {
		convertFromJsonOLAC("json/sample-olac-rule2.json");
//		XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "type" });
	}

	@Test
	public void buildFromJsonOLACRule3() {
		convertFromJsonOLAC("json/sample-olac-rule3.json");
//		XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "description" });
	}

	@Test
	public void buildFromJsonOLACRule4() {
		convertFromJsonOLAC("json/sample-olac-rule4.json");
//		XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "contributor", "creator" });
	}

	@Test
	public void buildFromJsonOLACRule5() {
		convertFromJsonOLAC("json/sample-olac-rule5.json");
//		XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "subject", "language" });
	}

	@Test
	public void buildFromJsonOLACRule6() {
		convertFromJsonOLAC("json/sample-olac-rule6.json");
//		XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "date" });
	}
}
