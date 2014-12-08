package fr.ortolang.diffusion.api.test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import fr.ortolang.diffusion.api.client.OrtolangRestClient;
import fr.ortolang.diffusion.api.client.OrtolangRestClientException;

public class WorkspaceTest {

	private static Logger logger = Logger.getLogger(WorkspaceTest.class.getName());
	private static OrtolangRestClient client;

	@BeforeClass
	public static void init() {
		client = new OrtolangRestClient("user1", "tagada", "http://localhost:8080/api/rest");
	}

	@AfterClass
	public static void shutdown() {
		client.close();
	}
	
	@Ignore
	@Test
	public void crud() throws IOException {
		logger.log(Level.INFO, "Checking CRUD on workspace");

		try {
			client.checkAuthentication();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "authentication failed: " + e.getMessage(), e);
		}
		
		String wskey = UUID.randomUUID().toString();
		
		try {
			boolean exists = client.objectExists(wskey);
			if ( exists ) {
				logger.log(Level.WARNING, "Workspace with key [" + wskey + "] already exists, exit");
				return;
			}
			logger.log(Level.INFO, "# Create workspace");
			client.createWorkspace(wskey, "bench", wskey + " complete name");
			
			logger.log(Level.INFO, "# Read workspace");
			JsonObject jsonWorkspaceObject = client.readWorkspace(wskey);
			logger.log(Level.INFO, "  Workspace infos : " + jsonWorkspaceObject);
			
			logger.log(Level.INFO, "# Create collections");
			client.writeCollection(wskey, "/a", "The A collection");
			client.writeCollection(wskey, "/a/b", "The A/B collection");
			client.writeCollection(wskey, "/a/b/b", "The A/B/B collection");
			client.writeCollection(wskey, "/a/b/b/a", "The A/B/B/A collection");
			
			logger.log(Level.INFO, "# Create dataobjects");
			client.writeDataObject(wskey, "/a/b/b/a/file1.txt", "This is a simple text file", new File(ClassLoader.getSystemResource("samples/file1.txt").getPath()), null);
			client.writeDataObject(wskey, "/a/b/b/a/file2.txt", "This is another simple text file", new File(ClassLoader.getSystemResource("samples/file1.txt").getPath()), null);
			
			logger.log(Level.INFO, "# Browse head");
			StringBuffer output = new StringBuffer();
			browseWorkspace(wskey, "head", "/", output);
			logger.log(Level.INFO, "Head content: \r\n" + output.toString());
			
			logger.log(Level.INFO, "# Snapshot Workspace");
			client.snapshotWorkspace(wskey, "version1");
			
			logger.log(Level.INFO, "# Browse head");
			output = new StringBuffer();
			browseWorkspace(wskey, "head", "/", output);
			logger.log(Level.INFO, "Head content: \r\n" + output.toString());
			
			logger.log(Level.INFO, "# Update dataobjects");
			client.writeDataObject(wskey, "/a/b/b/a/file1.txt", "This is a simple text file", new File(ClassLoader.getSystemResource("samples/file1.txt").getPath()), null);
			client.writeDataObject(wskey, "/a/b/b/a/file2.txt", "This is another very simple text file", new File(ClassLoader.getSystemResource("samples/file1.txt").getPath()), null);
			
			logger.log(Level.INFO, "# Browse head");
			output = new StringBuffer();
			browseWorkspace(wskey, "head", "/", output);
			logger.log(Level.INFO, "Head content: \r\n" + output.toString());
			
		} catch ( Exception e ) { 
			logger.log(Level.SEVERE, "unexpected error: " + e.getMessage(), e);
		}
		
		// CREATE METADATAOBJECT
		// READ COLLECTION
		// CREATE SNAPSHOT
		// LISTING SNAPSHOTS
		// DELETING WORKSPACE
		

	}
	
	public void browseWorkspace(String workspace, String branche, String path, StringBuffer buffer) throws OrtolangRestClientException {
		JsonObject element = client.getWorkspaceElement(workspace, branche, path);
		buffer.append("{").append(element.getInt("clock")).append("} [").append(element.getString("modification")).append("] ").append(path).append(" (").append(element.getString("description")).append(")\r\n");
		if ( element.getString("type").equals("collection")) {
			JsonArray children = element.getJsonArray("elements");
			for ( JsonValue child : children ) {
				String name = ((JsonObject)child).getString("name");
				if ( !path.endsWith("/") ) {
					browseWorkspace(workspace, branche, path + "/" + name, buffer);
				} else {
					browseWorkspace(workspace, branche, path + name, buffer);
				}
			}
		}
	}
}
