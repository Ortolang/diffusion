package fr.ortolang.diffusion.oai;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.XMLElement;
import fr.ortolang.diffusion.util.StreamUtils;

public class XMLDocumentTest {

	private static final Logger LOGGER = Logger.getLogger(XMLDocumentTest.class.getName());

	@Test
	public void removeHTMLTagWithNewlineTest() throws IOException {
		String content = StreamUtils.getContent(this.getClass().getResourceAsStream("/text.txt"));
		String newContent = XMLDocument.removeHTMLTag(content);
		LOGGER.log(Level.FINE,newContent);
		Assert.assertThat(newContent, CoreMatchers.not(CoreMatchers.containsString("<")));
	}

	public static void checkIfPresent(XMLDocument olac, String[] elementsName) {
		for(String elm : elementsName) {
			List<XMLElement> elms = olac.listFields(elm);
			if (elms.isEmpty()) {
				fail("Element '" + elm + "' must be present");
			}
		}
	}
}
