package fr.ortolang.diffusion.tool.treetagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
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

public class TreeTaggerInvokerTest {
	
	private Logger logger = Logger.getLogger(TreeTaggerInvokerTest.class.getName());
	private TreeTaggerInvoker invoker;
	private Mockery context;
	private CoreService core;
	private DataObject dataObject;

	@Before
	public void setup() {
		try {
			context = new Mockery();
			core = context.mock(CoreService.class);
			dataObject = new DataObject();
			
			invoker = new TreeTaggerInvoker();
			invoker.setCoreService(core);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
		
	@Test
	public void testInvoke() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Correct usage of treetagger.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K1");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PRO:PER\n"
				+"PRO:PER\n"
				+"VER:pres\n"
				+"DET:ART\n"
				+"NOM\n"
				+"SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeNoChunker() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with an input not chunked.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K1");
		params.put("tokenizer", "false");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "ABR\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeToken() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with -token option.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K1");
		params.put("token", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "Il	PRO:PER\n"
				+ "lui	PRO:PER\n"
				+ "donne	VER:pres\n"
				+ "une	DET:ART\n"
				+ "lettre	NOM\n"
				+ ".	SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeLemma() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with -lemma option.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K1");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K1");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PRO:PER	il\n"
				+ "PRO:PER	lui\n"
				+ "VER:pres	donner\n"
				+ "DET:ART	un\n"
				+ "NOM	lettre\n"
				+ "SENT	.\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeSGML() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with an input with SGML annotation.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K2"); 
		    will(returnValue(new ByteArrayInputStream("<SENTENCE><W TAG=\"PPS\">He</W><W TAG=\"VBZ\">books</W><W TAG=\"NNS\">tickets</W></SENTENCE>".getBytes())));
		    oneOf (core).readDataObject("K2");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("txt-input", "K2");
		params.put("sgml", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PP\nVVZ\nNNS\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeEosTag() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with an input with SGML annotation and eos tag option.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K2"); 
		    will(returnValue(new ByteArrayInputStream("<SENTENCE><W TAG=\"PPS\">He</W><W TAG=\"VBZ\">books</W><W TAG=\"NNS\">tickets</W></SENTENCE>".getBytes())));
		    oneOf (core).readDataObject("K2");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("txt-input", "K2");
		params.put("sgml", "true");
		params.put("eos-tag", "</W>");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PP\nNNS\nNNS\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeNoLg() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without a language specified.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K3"); will(returnValue(new ByteArrayInputStream("Ceci est une phrase clé.".getBytes())));
		    oneOf (core).readDataObject("K3");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("txt-input", "K3");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		assertThat(result.getLog(), containsString("parameter lg-input is mandatory"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokeNoInput() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without an input file specified.");    
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		assertThat(result.getLog(), containsString("parameter txt-input is mandatory"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}

	@Test
	public void testInvokeThreshold1() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with threshold option : p = 0.1.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); 
		    will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("threshold", "true");
		params.put("p", "0.1");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "	NAM	ADJ	NOM\n	NOM	VER:pres\n	SENT\n	PRO:PER\n	PRO:PER\n	VER:pres\n	DET:ART\n	NOM\n	SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeThreshold2() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with threshold option : p = 0.5.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("threshold", "true");
		params.put("p", "0.5");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "	NAM	ADJ	NOM\n	NOM\n	SENT\n	PRO:PER\n	PRO:PER\n	VER:pres\n	DET:ART\n	NOM\n	SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeThreshold3() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with threshold option and prob option.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("threshold", "true");
		params.put("p", "0.5");
		params.put("prob", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "	NAM 0.457527	ADJ 0.300671	NOM 0.241802\n"
				+ "	NOM 0.866092\n"
				+ "	SENT 1.000000\n"
				+ "	PRO:PER 1.000000\n"
				+ "	PRO:PER 0.999999\n"
				+ "	VER:pres 0.998933\n"
				+ "	DET:ART 0.996075\n"
				+ "	NOM 0.999963\n"
				+ "	SENT 1.000000\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeLexicalInfo1() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with lexical information option : proto.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("info-lex", "proto");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "ADJ	s ADJ NAM NOM\n"
				+ "NOM	f NOM VER:pres\n"
				+ "SENT	f SENT\n"
				+ "PRO:PER	f PRO:PER\n"
				+ "PRO:PER	f PRO:PER VER:pper\n"
				+ "VER:pres	f NOM VER:impe VER:pres VER:subp\n"
				+ "DET:ART	f DET:ART NOM NUM\n"
				+ "NOM	f NOM VER:impe VER:pres VER:subp\n"
				+ "SENT	f SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeLexicalInfo2() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with lexical information option : gramotron.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("info-lex", "gramotron");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "ADJ	 { ADJ, NAM, NOM }\n"
				+ "NOM	 { NOM, VER:pres }\n"
				+ "SENT	 SENT\n"
				+ "PRO:PER	 PRO:PER\n"
				+ "PRO:PER	 { PRO:PER, VER:pper }\n"
				+ "VER:pres	 { NOM, VER:impe, VER:pres, VER:subp }\n"
				+ "DET:ART	 { DET:ART, NOM, NUM }\n"
				+ "NOM	 { NOM, VER:impe, VER:pres, VER:subp }\n"
				+ "SENT	 SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokeLexicalInfo3() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with lexical information option : proto-with-prob.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K4"); will(returnValue(new ByteArrayInputStream("Léo part. Il lui donne une lettre.".getBytes())));
		    oneOf (core).readDataObject("K4");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "french");
		params.put("txt-input", "K4");
		params.put("info-lex", "proto-with-prob");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "ADJ	s ADJ 0.061 NAM 0.496 NOM 0.443\n"
				+ "NOM	f NOM 0.950 VER:pres 0.050\n"
				+ "SENT	f SENT 1.000\n"
				+ "PRO:PER	f PRO:PER 1.000\n"
				+ "PRO:PER	f PRO:PER 0.999 VER:pper 0.001\n"
				+ "VER:pres	f NOM 0.089 VER:impe 0.000 VER:pres 0.910 VER:subp 0.001\n"
				+ "DET:ART	f DET:ART 0.946 NOM 0.007 NUM 0.047\n"
				+ "NOM	f NOM 0.902 VER:impe 0.000 VER:pres 0.094 VER:subp 0.003\n"
				+ "SENT	f SENT 1.000\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePretagging() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PP\n"
				+ "VVD\n"
				+ "TO\n"
				+ "NP\n"
				+ "SENT\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePretagging2() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input, with -pt-with-lemma.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP	NYC\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("pt-with-lemma", "true");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "PP	he\n"
				+ "VVD	move\n"
				+ "TO	to\n"
				+ "NP	NYC\n"
				+ "SENT	.\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePretagging3() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input, with -pt-with-prob.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP	0.01\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("pt-with-prob", "true");
		params.put("threshold", "true");
		params.put("p", "0.1");
		params.put("prob", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "	PP 1.000000\n"
				+ "	VVD 0.993314\n"
				+ "	TO 0.999997\n"
				+ "	NP 1.000000\n"
				+ "	SENT 1.000000\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePretagging4() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input, with -pt-with-prob and -pt-with-lemma.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP	0.01	NYC\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("pt-with-lemma", "true");
		params.put("pt-with-prob", "true");
		params.put("lemma", "true");
		params.put("threshold", "true");
		params.put("p", "0.5");
		params.put("prob", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResult = "	PP he 1.000000\n"
				+ "	VVD move 0.993314\n"
				+ "	TO to 0.999997\n"
				+ "	NP NYC 1.000000\n"
				+ "	SENT . 1.000000\n";
		
		assertEquals(expectedResult, result.getOutput());
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePretaggingError1() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without pre-tagging input, with -pt-with-lemma.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("pt-with-lemma", "true");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		assertThat(result.getLog(), containsString("Missing lemma in tagger input at: New York City"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokePretaggingError2() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input, without -pt-with-lemma.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP	NYC\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		assertThat(result.getLog(), containsString("unknown tag <NYC>"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokePretaggingError3() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without pre-tagging input, with -pt-with-prob.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("pt-with-prob", "true");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		assertThat(result.getLog(), containsString("Missing probability in tagger input at: New York City"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokePretaggingError4() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger with pre-tagging input, without -pt-with-prob.");    
		dataObject.setName("test.txt");
		context.checking(new Expectations() {{
		    oneOf (core).download("K5"); will(returnValue(new ByteArrayInputStream("He\nmoved\nto\nNew York City	NP	0.01\n.\n".getBytes())));
		    oneOf (core).readDataObject("K5");
		    will(returnValue(dataObject));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "run-tt");
		params.put("lg-input", "english");
		params.put("tokenizer", "false");
		params.put("txt-input", "K5");
		params.put("lemma", "true");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		assertThat(result.getLog(), containsString("unknown tag <0.01>"));
		assertEquals(ToolInvokerResult.Status.ERROR, result.getStatus());
	}
	
	@Test
	public void testInvokePrintProbTree() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without -print-prob-tree option.");    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "print-prob-tree");
		params.put("lg-input", "french");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());
		
		String expectedResultStart = "tag[-1] = DET:ART\n"
				+"	tag[-2] = DET:ART\n"
				+"		  ABR 0.000417\n"
				+"		  ADJ 0.068912\n"
				+"		  ADV 0.004615\n"
				+"		DET:ART 0.000936\n"
				+"		DET:POS 0.000012\n"
				+"		  INT 0.000000\n"
				+"		  KON 0.143150\n"
				+"		  NAM 0.015153\n"
				+"		  NOM 0.469589\n"
				+"		  NUM 0.004218\n"
				+"		  PRO 0.000000";
		
		assertThat(result.getOutput(), containsString(expectedResultStart));
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void testInvokePrintSuffixTree() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		System.out.println("Call of treetagger without -print-suffix-tree option.");    
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("task", "print-suffix-tree");
		params.put("lg-input", "french");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, "log: " +result.getLog());

		//logger.log(Level.INFO, "result: " +result.getOutput());
		
		String expectedResultStart = "@ntes\n	ADJ 0.493157\n	NOM 0.220891\n	VER:subp 0.125933\n	VER:pres 0.112508\n	VER:pper 0.047511\n"
				+"astes\n	NOM 0.850877\n	ADJ 0.078947\n	VER:subp 0.035088\n	VER:pres 0.035088";
		
		assertThat(result.getOutput(), containsString(expectedResultStart));
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}
}
