package fr.ortolang.diffusion.tool.marsatag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.undertow.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class MarsaTagInvokerTest {
	
	private Logger logger = Logger.getLogger(MarsaTagInvokerTest.class.getName());
	private MarsaTagInvoker invoker;
	private Mockery context;
	private CoreService core;
	private static final String DEFAULT_FILES_FOLDER = "src/test/resources/tool/";

	@Before
	public void setup() {
		try {
			context = new Mockery();
			core = context.mock(CoreService.class);
			
			invoker = new MarsaTagInvoker();
			invoker.setCoreService(core);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
		
	@Test
	public void testInvokeTokenizer() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Correct usage of marsatag's tokenizer.");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "<?xml version='1.0' encoding='UTF-8'?>\n"
				+ "<?xml-stylesheet type=\"text/xsl\" href=\"mars.xsl\"?>\n"
					+ "<document format=\"MARS_1.0\">\n"
						+ "	<sample>\n"
							+ "<token form=\"Il\" regex_type=\"Unknown\"/>\n"
							+ "<token form=\"lui\" regex_type=\"Unknown\"/>\n"
							+ "<token form=\"donne\" regex_type=\"Unknown\"/>\n"
							+ "<token form=\"une\" regex_type=\"Unknown\"/>\n"
							+ "<token form=\"lettre\" regex_type=\"Unknown\"/>\n"
							+ "<token form=\".\" regex_type=\"punctuation_strong\"/>\n\n"
						+ "	</sample>\n"
					+ "</document>";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderRaw() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of marsatag with reader raw.");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "raw");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderTokens() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of marsatag with reader tokens.");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il\nlui\ndonne\nune\nlettre\n.\n".getBytes())));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "tokens");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderColumns() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader columns (.csv).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-columns.csv").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "columns");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Le/D- commis/Nc change/Vm-- la/D- vis/Nc à l'intérieur/R- du/SD- meuble/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderMARS() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader MARS (.xml).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-mars.xml").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "MARS");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		String expectedResult = "Léo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderSYNT() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader SYNT (.xml).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-synt.xml").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "SYNT");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		String expectedResult = "Léo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderPraatTextGrid() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader praat-textgrid.");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-praat.TextGrid").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "praat-textgrid");
		params.put("praat-tier", "S-token");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		String expectedResult = "Léo/Np part/Vm-- ./Cs Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Af\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderPraatTextGridText() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader praat-textgrid-text.");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-praat.TextGrid").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "praat-textgrid-text");
		params.put("praat-tier", "S-token");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		String expectedResult = "Léo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderStAX() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader StAx (universal reader for xml formats - TEI ).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-stax.xml").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "StAX");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		logger.log(Level.INFO, "log: " +result.getOutput());

		String expectedResult = "Leo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd";
		
		assertThat(result.getOutput(), containsString(expectedResult));
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderTikaRaw() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader tika raw (here a .doc).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.doc").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "tika-raw");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		logger.log(Level.INFO, "log: " +result.getOutput());

		String expectedResult = "Léo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeReaderTikaXHTML() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, IOException {
		System.out.println("Call of marsatag with reader tika tika-xhtml (here a .pdf).");    
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); 
		    will(returnValue(new BufferedInputStream(new FileInputStream(Paths.get(DEFAULT_FILES_FOLDER + "reader-test-tika.pdf").toFile()))));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("txt-input", "K1");
		params.put("tokenizer", "true");
		params.put("morphosyntaxer", "true");
		params.put("postagger", "true");
		params.put("reader", "tika-xhtml");
		params.put("writer", "pos");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		logger.log(Level.INFO, "log: " +result.getOutput());

		String expectedResult = "Léo/Np part/Vm-- ./Wd Il/Ppn lui/Ppd donne/Vm-- une/D- lettre/Nc ./Wd\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
}
