package fr.ortolang.diffusion.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class XmlUtilsTest {

//	@Test
	public void testValidation() throws SAXException, IOException, ParserConfigurationException {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("xml/permission.xml");
		String xml = StreamUtils.getContent(dcInputStream);
//		XmlUtils.validateXml(xml);
//		String xmlPath = "src/test/resources/xml/permission.xml";
//		XmlUtils.validation(xml);
	}
	
	@Test
	public void testValid() throws SAXException, IOException {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("xml/permission.xml");
		String xml = StreamUtils.getContent(dcInputStream);
		XmlUtils.validateXml(xml);
//		XmlUtils.valid("src/test/resources/xml/permission.xml");
	}
}
