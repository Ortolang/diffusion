package fr.ortolang.diffusion.test.bagit;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountManager;


public class BagItImporter {
	
	public static final String FILENAME_PATTERN = ".";
	public static final String PARENT_NAME_PATTERN = "..";
	
	private static Logger logger = Logger.getLogger(BagItImporter.class.getName());
	
	private String bagsList;
	private String bagsNamePattern;

	public BagItImporter(String bagsList) {
		this(bagsList, FILENAME_PATTERN);
	}
	
	public BagItImporter(String bagsList, String bagsNamePattern) {
		this.bagsList = bagsList;
		this.bagsNamePattern = bagsNamePattern;
	}
	
	public void perform() throws IOException, OrtolangClientException, OrtolangClientAccountException {
		
		if(bagsList==null) {
			throw new IOException("Parameter bagsFolder is mandatory");
		}
		
//		OrtolangClientAccountManager.getInstance("client").setCredentials("root", "tagada54");
		
		String[] bagsListSplit = bagsList.split(",");
		for ( String bag : bagsListSplit) {
			OrtolangClient client = new OrtolangClient("client");
			client.login("root");
			String profile = client.connectedProfile();
			logger.log(Level.INFO, "connected profile: {0}", profile);
			
			logger.log(Level.INFO, "Starting import a bag at "+bag);
			
			String bagName = "";
			if(bagsNamePattern.equals(FILENAME_PATTERN)) {
				bagName = bag.substring(bag.lastIndexOf('/')+1);
			} else if(bagsNamePattern.equals(PARENT_NAME_PATTERN)) {
				String[] bagSplit = bag.split("/");
				bagName = bagSplit[bagSplit.length-2];
			} else {
				bagName = bag.substring(bag.lastIndexOf('/')+1);
			}
			
			logger.log(Level.INFO, "Creating new import-workspace process for bag: " + bagName);
			String key = bagName.replaceFirst(".zip", "");
			Map<String, String> params = new HashMap<String, String> ();
			params.put("wskey", key);
			params.put("wsname", bagName);
			params.put("wstype", "benchmark");
			params.put("bagpath", bag);

			String pkey = client.createProcess("import-workspace", "Workspace import for bag " + bagName, params, Collections.<String, File> emptyMap());
			logger.log(Level.INFO, "process created with key : " + pkey);

			client.logout();
			client.close();
		}
	}
	
	
	public static void main(String[] argv) throws IOException, OrtolangClientException, OrtolangClientAccountException {
		
		String bagsList = null;
		String bagsNamePattern = BagItImporter.FILENAME_PATTERN;
		if(argv.length>0) {
			bagsList = argv[0];
		} else {
			throw new IOException("Bag file must be specify in first argument.");
		}
		
		if(argv.length>1) {
			bagsNamePattern = argv[1];
		}
		
		String username = System.getProperty("username", "root");
		String password = System.getProperty("password", "tagada54");
		
		OrtolangClientAccountManager.getInstance("client").setCredentials(username, password);
		BagItImporter importer = new BagItImporter(bagsList, bagsNamePattern);
		importer.perform();
	}
}
