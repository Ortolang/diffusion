package fr.ortolang.diffusion.oai;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.util.StreamUtils;

public class XMLDocumentTest {

	@Test
	public void removeHTMLTagWithNewlineTest() throws IOException {
		String content = StreamUtils.getContent(this.getClass().getResourceAsStream("/text.txt"));
		Assert.assertThat(XMLDocument.removeHTMLTag(content), CoreMatchers.not(CoreMatchers.containsString("<")));
	}
}
