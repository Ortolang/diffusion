package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.oai.format.OAI_DC;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.util.StreamUtils;

public class OAI_DCTest {

	private static final Logger LOGGER = Logger.getLogger(OAI_DCTest.class.getName());

	@Test
	public void buildFromJsonDC() {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-dc.json");
		try {
			String dc_json = StreamUtils.getContent(dcInputStream);
			OAI_DC oai_dc = OAI_DCFactory.buildFromJson(dc_json);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC");
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc,
					new String[] { "title", "description", "coverage", "subject", "identifier" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void convertFromJsonOLAC() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json OLAC");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc,
					new String[] { "title", "description", "coverage", "subject", "identifier", "type" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLACRule2() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac-rule2.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC (checks rule 2)");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "type" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLACRule3() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac-rule3.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC (checks rule 3)");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "description" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLACRule4() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac-rule4.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC (checks rule 4)");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "contributor", "creator" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLACRule5() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac-rule5.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC (checks rule 5)");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "subject", "language" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void buildFromJsonOLACRule6() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac-rule6.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			LOGGER.log(Level.INFO, "Build OAI_DC from json DC (checks rule 6)");
			OAI_DC oai_dc = OAI_DCFactory.convertFromJsonOlac(olac_json);
			LOGGER.log(Level.INFO, oai_dc.toString());
			XMLDocumentTest.checkIfPresent(oai_dc, new String[] { "date" });
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
