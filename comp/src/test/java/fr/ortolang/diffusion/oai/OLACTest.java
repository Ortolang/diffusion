package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import fr.ortolang.diffusion.oai.format.OLAC;
import fr.ortolang.diffusion.oai.format.OLACFactory;
import fr.ortolang.diffusion.oai.format.XMLElement;
import fr.ortolang.diffusion.util.StreamUtils;

public class OLACTest {

	public OLACTest() {
	}

	@Test
	public void buildFromJson() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac.json");
		try {
			String olac_json = StreamUtils.getContent(olacInputStream);
			OLAC olac = OLACFactory.buildFromJson(olac_json);
			checkIfPresent(olac, new String[] {"title", "description", "abstract", "coverage", "subject", "identifier"});
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void checkIfPresent(OLAC olac, String[] elementsName) {
		for(String elm : elementsName) {
			List<XMLElement> elms = olac.listFields(elm);
			if (elms.isEmpty()) {
				fail("Element '" + elm + "' must be present");
			}
		}
	}
}
