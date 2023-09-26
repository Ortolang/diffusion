package fr.ortolang.diffusion.api.oai;

import java.io.IOException;
import java.io.InputStream;

import org.dom4j.DocumentException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import fr.ortolang.diffusion.api.oai.metadata.GenericOaiXmlDocument;
import fr.ortolang.diffusion.api.oai.metadata.GenericOaiXmlParser;
import fr.ortolang.diffusion.api.oai.metadata.cmdi.CmdiDocument;
import fr.ortolang.diffusion.api.oai.metadata.cmdi.CmdiParser;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;
import fr.ortolang.diffusion.util.StreamUtils;

public class OaiTemplateTest {
    @Test
    public void testCmdiTemplate() throws TemplateEngineException, IOException, DocumentException {
        InputStream cmdiInputStream = getClass().getClassLoader().getResourceAsStream("cmdi.xml");
		String xml = StreamUtils.getContent(cmdiInputStream);
        CmdiDocument cmdiDoc = CmdiParser.newInstance().parse(xml).getDoc();
        RecordRepresentation recordRepresentation = new RecordRepresentation();
        recordRepresentation.setIdentifier("oai:ortolang.fr:fb858381-c212-47e5-b387-a11ed1fc8276");
        OaiRecordRepresentation representation = new OaiRecordRepresentation(
            "/api/oai",
            recordRepresentation,
            cmdiDoc
        );
        assertEquals(9, cmdiDoc.getValues().size());
        
		String result = TemplateEngine.getInstance(this.getClass().getClassLoader()).process("cmdi", representation);
		System.out.println(result);
    }

    @Test
    public void testOaiDcTemplate() throws TemplateEngineException, IOException, DocumentException {
        InputStream cmdiInputStream = getClass().getClassLoader().getResourceAsStream("oai_dc.xml");
		String xml = StreamUtils.getContent(cmdiInputStream);
        GenericOaiXmlDocument oaiDcDoc = GenericOaiXmlParser.newInstance().parse(xml).getDoc();
        RecordRepresentation recordRepresentation = new RecordRepresentation();
        recordRepresentation.setIdentifier("oai:ortolang.fr:fb858381-c212-47e5-b387-a11ed1fc8276");
        OaiRecordRepresentation representation = new OaiRecordRepresentation(
            "/api/oai",
            recordRepresentation,
            oaiDcDoc
        );
        assertEquals(9, oaiDcDoc.getValues().size());
        
		String result = TemplateEngine.getInstance(this.getClass().getClassLoader()).process("oai_dc", representation);
		System.out.println(result);
    }

    @Test
    public void testOlacTemplate() throws TemplateEngineException, IOException, DocumentException {
        InputStream cmdiInputStream = getClass().getClassLoader().getResourceAsStream("olac.xml");
		String xml = StreamUtils.getContent(cmdiInputStream);
        GenericOaiXmlDocument olacDoc = GenericOaiXmlParser.newInstance().parse(xml).getDoc();
        RecordRepresentation recordRepresentation = new RecordRepresentation();
        recordRepresentation.setIdentifier("oai:ortolang.fr:fb858381-c212-47e5-b387-a11ed1fc8276");
        OaiRecordRepresentation representation = new OaiRecordRepresentation(
            "/api/oai",
            recordRepresentation,
            olacDoc
        );
        assertEquals(9, olacDoc.getValues().size());
        
		String result = TemplateEngine.getInstance(this.getClass().getClassLoader()).process("olac", representation);
		System.out.println(result);
    }
}
