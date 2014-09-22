package fr.ortolang.diffusion.api.bench;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;

import org.junit.Test;

import fr.ortolang.diffusion.api.client.OrtolangRestClient;

public class PlateformBench {
	
	private static final String DEFAULT_BAGS_FOLDER = "/media/space/jerome/Data/bags-small";
	private static final int DEFAULT_ITERATIONS = 1;
	
	private static Logger logger = Logger.getLogger(PlateformBench.class.getName());
	
	@Test
	public void bench() throws IOException {
		
		File bags = getBagsFolder();
		int iterations = getIterations();
		String suffix = getWorkspaceSuffix();
		
		logger.log(Level.INFO, "Starting palteform benchmark using bags folder: " + bags.getAbsolutePath() + ", workspace suffix: " + suffix);
		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://diffusion.ortolang.fr/api/rest");
		
		for ( int i=1; i<=iterations; i++ ) {
			logger.log(Level.INFO, "Starting new import iteration: " + i);
			List<File> files;
			if ( getBagsOrder() != null ) {
				files = new ArrayList<File> ();
				for ( String bag : getBagsOrder().split(",") ) {
					files.add(new File(bags, bag));
				}
			} else {
				files = Arrays.asList(bags.listFiles());
			}
			for ( File bag : files) {
				if ( !bag.isDirectory() ) {
					logger.log(Level.INFO, "Creating new import-workspace process for bag: " + bag.getName());
					String key = bag.getName() + "." + i + "." + getWorkspaceSuffix();
					Map<String, String> params = new HashMap<String, String> ();
					params.put("workspace-key", key);
					params.put("workspace-name", "Workspace of bag " + bag.getName());
					params.put("workspace-type", "benchmark");
					Map<String, File> attachments = new HashMap<String, File> ();
					attachments.put("bag-hash", bag);
					try {
						String pkey = client.createProcess("import-workspace", "Workspace import num " + i + " for bag " + bag.getName(), params, attachments);
						logger.log(Level.INFO, "process created with key : " + pkey + " watching process progression");
						boolean finished = false;
						while ( !finished ) {
							try {
								Thread.sleep(120000);
							} catch ( InterruptedException e ) {
								logger.log(Level.WARNING, "thread sleep interrupted: " + e.getMessage());
							}
							JsonObject process = client.getProcess(pkey);
							String state = process.getString("state");
							if ( state.equals("ABORTED") || state.equals("COMPLETED") ) {
								logger.log(Level.INFO, "process ended, process log: \r\n" + process.getString("log"));
								finished = true;
							} else {
								logger.log(Level.INFO, "process in progress, waiting...");
							}
						}
					} catch ( Exception e ) {
						e.printStackTrace();
						logger.log(Level.WARNING, "unable to create process for bag " + bag.getName());
					}
				}
			}
		}
		
	}
	
	private File getBagsFolder() {
		File folder = Paths.get(DEFAULT_BAGS_FOLDER).toFile();
		String property = System.getProperty("bags.folder");
		if (property != null && property.length() != 0) {
			folder = Paths.get(property).toFile();
		} 
		return folder;
	}
	
	private String getBagsOrder() {
		String property = System.getProperty("bags.order");
		if (property != null && property.length() != 0) {
			return property;
		}
		return null;
	}
	
	private String getWorkspaceSuffix() {
		String suffix = "1";
		String property = System.getProperty("workspace.suffix");
		if (property != null && property.length() != 0) {
			suffix = property;
		}
		return suffix;
	}
	
	private int getIterations() {
		int iterations = DEFAULT_ITERATIONS;
		String property = System.getProperty("iterations");
		if (property != null && property.length() != 0) {
			iterations = Integer.parseInt(property);
		}
		return iterations;
	}
	
}
