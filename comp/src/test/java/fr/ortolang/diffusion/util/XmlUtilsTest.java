package fr.ortolang.diffusion.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.SAXException;

public class XmlUtilsTest {

	@Test
	public void testValid() throws SAXException, IOException {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("xml/permission.xml");
		String xml = StreamUtils.getContent(dcInputStream);
		XmlUtils.validateXml(xml);
	}

	@Test
	public void testSipValid() throws SAXException, IOException {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("sip/sip.xml");
		String xml = StreamUtils.getContent(dcInputStream);
		XmlUtils.validateXml(xml);
	}
}
