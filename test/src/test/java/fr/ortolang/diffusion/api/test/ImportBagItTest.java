package fr.ortolang.diffusion.api.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

import fr.ortolang.diffusion.api.bagit.BagItImporter;
import fr.ortolang.diffusion.api.client.OrtolangRestClient;
import fr.ortolang.diffusion.api.client.OrtolangRestClientException;
import fr.ortolang.diffusion.api.config.ClientConfig;

/**
 * Tests when imports a bagit archive.
 * @author cyril
 *
 */
public class ImportBagItTest {

	@Test
	public void bench() throws IOException, OrtolangRestClientException {
		File bagsFolder = getBagsFolder();
//		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://192.168.32.6/api/rest");
		
		BagItImporter importer = new BagItImporter(client, bagsFolder);
		importer.perform();
	}
	
	private File getBagsFolder() {
		File folder = Paths.get(BagItImporter.DEFAULT_BAGS_FOLDER).toFile();
		String property = System.getProperty("bags.folder");
		if (property != null && property.length() != 0) {
			folder = Paths.get(property).toFile();
		} 
		return folder;
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
