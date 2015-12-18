package fr.ortolang.diffusion.store.json;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OrtolangKeyExtractorTest {

	private static final Logger LOGGER = Logger.getLogger(OrtolangKeyExtractorTest.class.getName());
	
	@Test
	public void testExtractOneOrtolangKey() {
		String json = "${referential:atilf-_1}";

		LOGGER.log(Level.INFO, "extract from : "+json);
		List<String> keys = OrtolangKeyExtractor.extractOrtolangKeys(json);
		assertEquals(1, keys.size());
		
		LOGGER.log(Level.INFO, "ortolang key : "+keys.get(0));
	}

	@Test
	public void testExtractTwoOrtolangKey() {
		String json = "${referential:atilf-_1} bidule ${referential:atilf-_2}";
		LOGGER.log(Level.INFO, "extract from : "+json);
		List<String> keys = OrtolangKeyExtractor.extractOrtolangKeys(json);
		assertEquals(2, keys.size());
		
		LOGGER.log(Level.INFO, "ortolang key : "+keys.get(0));
		LOGGER.log(Level.INFO, "ortolang key : "+keys.get(1));
	}
}
