package fr.ortolang.diffusion.api.bench;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.api.builder.WorkspaceBuilder;

public class WorkspaceBench {

	private static final String DEFAULT_DATASET_FOLDER = "/media/space/jerome/Data/dataset";
	private static final int DEFAULT_USER_RANGE = 4;
	private static final int DEFAULT_ITERATIONS = 1;
	
	private static Logger logger = Logger.getLogger(WorkspaceBench.class.getName());
	
//	@Test
//	public void bench() throws IOException {
//		
//		File data = getBaseDataFolder();
//		int userrange = getUserRange();
//		int iterations = getIterations();
//		String prefix = getWorkspacePrefix();
//		
//		logger.log(Level.INFO, "Starting benchmark using data folder: " + data.getAbsolutePath() + ", user range of: " + userrange + ", workspace prefix: " + prefix + " and with " + iterations + " iterations.");
//		
//		Random rand = new Random();
//		for ( int i=0; i<iterations; i++ ) {
//			logger.log(Level.INFO, "Starting new iteration number: " + i);
//			for ( File dataset : data.listFiles()) {
//				if ( dataset.isDirectory() ) {
//					String username = "user" + (rand.nextInt(userrange)+1);
//					logger.log(Level.INFO, "Starting new workspace import for found folder: " + dataset.getName() + " as user: " + username);
//					long start = System.currentTimeMillis();
//					WorkspaceBuilder builder = new WorkspaceBuilder(username, "tagada", "http://localhost:8080/api/rest", prefix + "." + i + "." + dataset.getName(), dataset.getAbsolutePath());
//					builder.run();
//					long stop = System.currentTimeMillis();
//					logger.log(Level.INFO, "Workspace [" + dataset.toString() + "] imported in " + (stop-start) + " ms");
//				} 
//			}
//		}
//		
//	}
//	
//	
//	private File getBaseDataFolder() {
//		File folder = Paths.get(DEFAULT_DATASET_FOLDER).toFile();
//		String property = System.getProperty("dataset.folder");
//		if (property != null && property.length() != 0) {
//			folder = Paths.get(property).toFile();
//		} 
//		return folder;
//	}
//	
//	private int getUserRange() {
//		int range = DEFAULT_USER_RANGE;
//		String property = System.getProperty("user.range");
//		if (property != null && property.length() != 0) {
//			range = Integer.parseInt(property);
//		} 
//		return range;
//	}
//	
//	private String getWorkspacePrefix() {
//		String prefix = "";
//		String property = System.getProperty("workspace.prefix");
//		if (property != null && property.length() != 0) {
//			prefix = property;
//		}
//		return prefix;
//	}
//	
//	private int getIterations() {
//		int iterations = DEFAULT_ITERATIONS;
//		String property = System.getProperty("iterations");
//		if (property != null && property.length() != 0) {
//			iterations = Integer.parseInt(property);
//		}
//		return iterations;
//	}

}
