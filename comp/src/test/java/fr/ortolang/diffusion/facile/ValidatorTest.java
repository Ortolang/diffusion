package fr.ortolang.diffusion.facile;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import fr.ortolang.diffusion.archive.facile.entity.Validator;
import fr.ortolang.diffusion.util.StreamUtils;

public class ValidatorTest {
    
	@Test
	public void testValid() throws IOException, JAXBException {
		InputStream dcInputStream = getClass().getClassLoader().getResourceAsStream("facile/validator_texte.xml");
		String xml = StreamUtils.getContent(dcInputStream);
        
        Validator validator = null;
        validator = Validator.fromXML(xml);
        System.out.println(validator);
        assertTrue(validator.getValid());
	}
}
