package fr.ortolang.diffusion.test.bagit;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

/**
 * Tests when imports a bagit archive.
 * @author cyril
 *
 */
public class ImportBagItTest {
	private static Logger logger = Logger.getLogger(ImportBagItTest.class.getName());
	
	@Test
	public void bench() throws IOException, OrtolangClientException, OrtolangClientAccountException {
		String bagsList = getBagsList();
		
		BagItImporter importer = new BagItImporter(bagsList);
		importer.perform();
	}
	
	private String getBagsList() {
		String list = BagItImporter.DEFAULT_BAGS_FOLDER;
		String property = System.getProperty("bags.list");
		logger.log(Level.INFO,"Property bags.list : "+property);
		if (property != null && property.length() != 0) {
			logger.log(Level.INFO,"Sets list to "+property);
			list = property;
		} 
		return list;
	}
}
