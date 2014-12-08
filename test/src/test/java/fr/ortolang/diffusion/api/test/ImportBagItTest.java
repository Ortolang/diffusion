package fr.ortolang.diffusion.api.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

import fr.ortolang.diffusion.api.bagit.BagItImporter;

/**
 * Tests when imports a bagit archive.
 * @author cyril
 *
 */
public class ImportBagItTest {

	@Test
	public void bench() throws IOException {
		File bagsFolder = getBagsFolder();

		BagItImporter importer = new BagItImporter(bagsFolder);
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
}
