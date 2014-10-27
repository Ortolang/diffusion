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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class TreeTaggerInvokerTest {
	
	private Logger logger = Logger.getLogger(TreeTaggerInvokerTest.class.getName());
	private TreeTaggerInvoker invoker;
	private Mockery context;
	private CoreService core;

	@Before
	public void setup() {
		try {
			context = new Mockery();
			core = context.mock(CoreService.class);
			
			invoker = new TreeTaggerInvoker();
			invoker.setCoreService(core);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testInvoke() throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException {
		context.checking(new Expectations() {{
		    oneOf (core).download("K1"); will(returnValue(new ByteArrayInputStream("Ceci est une phrase clé.".getBytes())));
		}});
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("lg-input", "french");
		params.put("txt-input", "K1");
		ToolInvokerResult result = invoker.invoke(params);
		
		logger.log(Level.INFO, result.getLog());
		
		assertEquals(ToolInvokerResult.Status.SUCCESS, result.getStatus());
	}

}
