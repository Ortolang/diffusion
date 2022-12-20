package fr.ortolang.diffusion.archive;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import fr.ortolang.diffusion.archive.aip.entity.Aip;
import fr.ortolang.diffusion.util.StreamUtils;

public class AipTest {
    @Test
    public void testFromXML() throws JAXBException, IOException, XMLStreamException {
        InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("archive/aip_test.xml");
		String xml = StreamUtils.getContent(dcInputStream);

        Aip aip = Aip.fromXML(xml);
        assertEquals("ark:/87895/1.19-1519937", aip.getDocDc().getIdentifier());
        assertEquals("1519937", aip.getDocMeta().getIdentifiantDocPac());
    }
}
