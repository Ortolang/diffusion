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
	
	private File bagsFolder;
	private OrtolangRestClient client;
	
	public BagItImporter(OrtolangRestClient client, File bagsFolder) {
		this.client = client;
		this.bagsFolder = bagsFolder;
	}
	
	public void perform() throws IOException, OrtolangRestClientException {
		
		if(bagsFolder==null) {
			throw new IOException("Parameter bagsFolder is mandatory");
		}
		
		logger.log(Level.INFO, "Starting import bags folder: " + bagsFolder.getAbsolutePath());
//		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
		
		List<File> files = Arrays.asList(bagsFolder.listFiles());
		
		for ( File bag : files) {
			if ( !bag.isDirectory() ) {
				logger.log(Level.INFO, "Creating new import-workspace process for bag: " + bag.getName());
				String key = bag.getName().replaceFirst(".zip", "");
				Map<String, String> params = new HashMap<String, String> ();
				params.put("wskey", key);
				params.put("wsname", "Workspace of bag " + bag.getName());
				params.put("wstype", "benchmark");
				params.put("bagpath", bag.getAbsolutePath());
				Map<String, File> attachments = null;
//				Map<String, File> attachments = new HashMap<String, File> ();
//				attachments.put("bagpath", bag);
//				try {
					String pkey = client.createProcess("import-workspace", "Workspace import for bag " + bag.getName(), params, attachments);
					logger.log(Level.INFO, "process created with key : " + pkey);
//					boolean finished = false;
//					while ( !finished ) {
//						try {
//							logger.log(Level.INFO, "Waiting for the import process...");
//							Thread.sleep(30000);
//						} catch ( InterruptedException e ) {
//							logger.log(Level.WARNING, "thread sleep interrupted: " + e.getMessage());
//						}
//						JsonObject process = client.getProcess(pkey);
//						String state = process.getString("state");
//						if ( state.equals("ABORTED") || state.equals("COMPLETED") ) {
//							logger.log(Level.INFO, "process ended, process log: \r\n" + process.getString("log"));
//							finished = true;
//						} else {
//							logger.log(Level.INFO, "process in progress, waiting...");
//						}
//					}
//				} catch ( Exception e ) {
//					e.printStackTrace();
//					logger.log(Level.WARNING, "unable to create process for bag " + bag.getName());
//					continue;
//				}
				
				
			}
		}
	}
	
	
	public static void main(String[] argv) throws IOException, OrtolangRestClientException {
		
		String bagsFolder = DEFAULT_BAGS_FOLDER;
		if(argv.length>0) {
			bagsFolder = argv[0];
		}
		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
		BagItImporter importer = new BagItImporter(client, new File(bagsFolder));
		importer.perform();
	}
}
