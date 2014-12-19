package fr.ortolang.diffusion.tool.job.test;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.tool.job.client.ToolJobRestClient;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;
import fr.ortolang.diffusion.tool.resource.GenericCollectionRepresentation;
import fr.ortolang.diffusion.tool.resource.ToolDescription;

/**
 * Test class for {@link fr.ortolang.diffusion.tool.resource.ToolResource} and {@link fr.ortolang.diffusion.tool.invoke.ToolJobService}.
 */
public class ToolJobServiceTest {	
 	
	static final String ROOT_URL = "http://localhost:8080/tool-tika/tika/"; 
	//static final String DIFFUSION_URL = "http://localhost:8080/api/rest/"; 

	private static Logger logger = Logger.getLogger(ToolJobServiceTest.class.getName());
	private static ResourceBundle bundle;
	private static ToolJobRestClient client;
	//private static OrtolangDiffusionRestClient clientDiffusion;

	@BeforeClass
	public static void init() {
		client = new ToolJobRestClient(ROOT_URL);
		//clientDiffusion = new OrtolangDiffusionRestClient(DIFFUSION_URL);
		bundle = ResourceBundle.getBundle("description", Locale.forLanguageTag("fr"));  
	}

	@AfterClass
	public static void shutdown() {
		client.close();
		//clientDiffusion.close();
	}
   		
	/**
	 * Test method for {@link fr.ortolang.diffusion.tool.resource.ToolResource#description(String)}.
	 */
	@Test
    public void testDescription() {
		logger.log(Level.INFO, "Testing get /description");		
		try {
			ToolDescription object = client.getDescription();
			assertEquals(bundle.getString("name"), object.getName());		
			assertEquals(bundle.getString("description"), object.getDescription());		
			assertEquals(bundle.getString("documentation"), object.getDocumentation());
			
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "Get tool description of " + bundle.getString("name") + " failed: " + e.getMessage(), e);
			fail("Get tool description of " + bundle.getString("name") + " failed: " + e.getMessage());
		}
    }
	
	/**
	 * Test method for {@link fr.ortolang.diffusion.tool.resource.ToolResource#executionForm()}.
	 */
	@Test
    public void testExecutionForm() {
		logger.log(Level.INFO, "Testing get /execution-form");		
		try {
			JsonArray object = client.getExecutionForm();
			InputStream is = getClass().getClassLoader().getResourceAsStream("execute.json");
			JsonArray config = Json.createReader(new StringReader(IOUtils.toString(is))).readArray();
			assertEquals(config, object);		
			
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "Get tool execution form of " + bundle.getString("name") + " failed: " + e.getMessage(), e);
			fail("Get tool execution form of " + bundle.getString("name") + " failed: " + e.getMessage());
		}
    }
		
	/**
	 * Test method for {@link fr.ortolang.diffusion.tool.resource.ToolResource#executions()}.
	 */
	@Test
    public void testExecutions() {
		logger.log(Level.INFO, "Testing post and get /jobs");		
		try {
			Map<String,String> params = new HashMap<String,String>();
			String input = new String( "0f83ac49-b6dd-4363-b2e2-26c839cae023");
			String output = new String( "metadata" );
			params.put( "input", input);
			params.put( "output", output);
			String name = "Tika";
			int p = 1;
			client.postExecutions(name, p, params);
			GenericCollectionRepresentation<ToolJob> object = client.getExecutions();
			for (ToolJob toolJob : object.getEntries()) {
				System.out.println("job : " + toolJob.getName() + " - " + toolJob.getStatus());
				assertEquals(name, toolJob.getName());
				assertEquals(p, toolJob.getPriority());
				assertEquals(input, toolJob.getParameter("input"));
				assertEquals(output, toolJob.getParameter("output"));
			}
			
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "Post/Get execution's job list  of " + bundle.getString("name") + " failed: " + e.getMessage(), e);
			fail("Post/Get execution's job list of " + bundle.getString("name") + " failed: " + e.getMessage());
		}
    }
}