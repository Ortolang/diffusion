package fr.ortolang.diffusion.tool.tika;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.undertow.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class TikaInvokerTest {
	
	private Logger logger = Logger.getLogger(TikaInvokerTest.class.getName());
	private TikaInvoker invoker;
	private Mockery context;
	private CoreService core;
	private DataObject dataObject;
	private static final String DEFAULT_FILES_FOLDER = "src/test/resources/tool/";

	@Before
	public void setup() {
		try {
			context = new Mockery();
			core = context.mock(CoreService.class);
			dataObject = new DataObject();
			
			invoker = new TikaInvoker();
			invoker.setCoreService(core);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testInvoke() throws Exception {
		System.out.println("Default output.");  
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Léo part. Il lui donne une lettre.";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeExtract() throws Exception {
		System.out.println("Output content to String.");  
		dataObject.setName("test.xml");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.doc").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("output", "extract");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Léo part. Il lui donne une lettre.";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
		
	@Test
	public void testInvokeMetadata() throws Exception {
		System.out.println("Output metadatas of a file.");  
		dataObject.setName("test.pdf");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("output", "metadata");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Content-Type:	application/pdf\n"
				+ "Creation-Date:	2014-11-06T11:18:24Z\n"
				+ "created:	Thu Nov 06 12:18:24 CET 2014\n"
				+ "dcterms:created:	2014-11-06T11:18:24Z\n"
				+ "meta:creation-date:	2014-11-06T11:18:24Z\n"
				+ "producer:	LibreOffice 4.2\n"
				+ "resourceName:	test.pdf\n"
				+ "xmp:CreatorTool:	Writer\n"
				+ "xmpTPg:NPages:	1\n";
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeLanguage() throws Exception {
		System.out.println("Output language of a file.");  
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("output", "language");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "fr";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeEncoding() throws Exception {
		System.out.println("Output encoding of a file.");  
		dataObject.setName("test.doc");
		context.checking(new Expectations() {{
		    oneOf (core).download("K2"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.doc").toFile()))));
		    oneOf (core).readDataObject("K2");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K2");
		params.put("output", "encoding");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "windows-1250";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeUnknownEncoding() throws Exception {
		System.out.println("Output encoding of a file.");  
		dataObject.setName("test.doc");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("output", "encoding");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		assertThat(result.getLog(), containsString("Charset detected as IBM420_ltr but the JVM does not support this, detection skipped"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokeMimetype() throws Exception {
		System.out.println("Output mimetype of a file.");  
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});	    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("output", "detect");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "application/pdf";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
}
