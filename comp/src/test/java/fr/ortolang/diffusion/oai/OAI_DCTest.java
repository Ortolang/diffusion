package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.ortolang.diffusion.oai.format.OAI_DC;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.util.StreamUtils;

public class OAI_DCTest {

	@Test
	public void buildFromJsonDC() {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-dc.json");
		try {
			String dc_json = StreamUtils.getContent(dcInputStream);
			OAI_DC oai_dc = OAI_DCFactory.buildFromJson(dc_json);
			System.out.println(oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] {"title", "description", "coverage", "subject", "identifier"});
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLAC() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			OAI_DC oai_dc = OAI_DCFactory.buildFromJson(olac_json);
			System.out.println(oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] {"title", "description", "coverage", "subject", "identifier", "type"});
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
