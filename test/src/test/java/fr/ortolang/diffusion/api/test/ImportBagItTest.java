package fr.ortolang.diffusion.api.test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.api.bagit.BagItImporter;
import fr.ortolang.diffusion.client.api.rest.OrtolangRestClient;
import fr.ortolang.diffusion.client.api.rest.OrtolangRestClientException;

/**
 * Tests when imports a bagit archive.
 * @author cyril
 *
 */
public class ImportBagItTest {
	private static Logger logger = Logger.getLogger(ImportBagItTest.class.getName());
	
	@Test
	public void bench() throws IOException, OrtolangRestClientException {
		String bagsList = getBagsList();
		OrtolangRestClient client = new OrtolangRestClient();
		
		BagItImporter importer = new BagItImporter(client, bagsList);
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

//	private String getHostname() {
//		String server_address = ClientConfig.SERVER_ADDRESS;
//		String property = System.getProperty("server.address");
//		if (property != null && property.length() != 0) {
//			folder = Paths.get(property).toFile();
//		} 
//		return folder;
//	}
}
