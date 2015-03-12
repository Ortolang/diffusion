package fr.ortolang.diffusion.client;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountManager;

public class ImportBagsTest {

	private static Logger logger = Logger.getLogger(ImportBagsTest.class.getName());

	@Test
	public void importBags() throws IOException, OrtolangClientException, OrtolangClientAccountException {
		String bags = System.getProperty("bags.list");
		logger.log(Level.INFO, "Property bags.list : " + bags);
		if (bags == null) {
			bags = "";
		}

		OrtolangClient client = new OrtolangClient("client");
		OrtolangClientAccountManager.getInstance("client").setCredentials("root", "tagada54");
		client.login("root");
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		
		for (String bag : bags.split(",")) {
			logger.log(Level.INFO, "Create process to import bag: " + bag);
			String name = bag.substring(bag.lastIndexOf('/') + 1);
			String key = name.replaceFirst(".zip", "");
			Map<String, String> params = new HashMap<String, String>();
			params.put("wskey", key);
			params.put("wsname", name);
			params.put("wstype", "import");
			params.put("bagpath", bag);
			client.createProcess("import-workspace", "Import " + name, params, Collections.<String, File> emptyMap());
		}

		client.logout();
		client.close();
	}
}
