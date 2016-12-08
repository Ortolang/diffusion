package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import fr.ortolang.diffusion.oai.format.OLAC;
import fr.ortolang.diffusion.oai.format.OLACFactory;
import fr.ortolang.diffusion.oai.format.XMLElement;

public class OLACTest {

	public OLACTest() {
	}

	@Test
	public void buildFromJson() {
		InputStream olacInputStream = getClass().getClassLoader().getResourceAsStream("json/sample-olac.json");
		try {
			String olac_json = getContent(olacInputStream);
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

    private String getContent(InputStream is) throws IOException {
        String content = null;
        try {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
        	System.err.println("unable to get content from stream");
        	e.printStackTrace();
        } finally {
            is.close();
        }
        return content;
    }

}
