package fr.ortolang.diffusion.api.bagit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.api.client.OrtolangRestClient;
import fr.ortolang.diffusion.api.client.OrtolangRestClientException;

public class BagItImporter {

	public static final String DEFAULT_BAGS_FOLDER = "src/test/resources/samples/bagit";
	
	private static Logger logger = Logger.getLogger(BagItImporter.class.getName());
	
	private String bagsList;
	private OrtolangRestClient client;
	
	public BagItImporter(OrtolangRestClient client, String bagsList) {
		this.client = client;
		this.bagsList = bagsList;
	}
	
	public void perform() throws IOException, OrtolangRestClientException {
		
		if(bagsList==null) {
			throw new IOException("Parameter bagsFolder is mandatory");
		}
		
//		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
		
//		List<File> files = Arrays.asList(bagsList.listFiles());
		String[] bagsListSplit = bagsList.split(",");
		for ( String bag : bagsListSplit) {
			logger.log(Level.INFO, "Starting import a bag at "+bag);
			String bagName = bag.substring(bag.lastIndexOf('/')+1);
			
			logger.log(Level.INFO, "Creating new import-workspace process for bag: " + bagName);
			String key = bagName.replaceFirst(".zip", "");
			Map<String, String> params = new HashMap<String, String> ();
			params.put("wskey", key);
			params.put("wsname", "Workspace of bag " + bagName);
			params.put("wstype", "benchmark");
			params.put("bagpath", bag);
			Map<String, File> attachments = null;

			String pkey = client.createProcess("import-workspace", "Workspace import for bag " + bagName, params, attachments);
			logger.log(Level.INFO, "process created with key : " + pkey);
			
		}
	}
	
	
	public static void main(String[] argv) throws IOException, OrtolangRestClientException {
		
		String bagsList = DEFAULT_BAGS_FOLDER;
		if(argv.length>0) {
			bagsList = argv[0];
		}
		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
		BagItImporter importer = new BagItImporter(client, bagsList);
		importer.perform();
	}
}
