package fr.ortolang.diffusion.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.client.auth.AuthenticationException;
import fr.ortolang.diffusion.client.auth.AuthenticationManager;
import fr.ortolang.diffusion.membership.MembershipService;

public class OrtolangClientTest {
	
	private static Logger logger = Logger.getLogger(OrtolangClientTest.class.getName());
	
	@BeforeClass
	public static void init() throws AuthenticationException {
		AuthenticationManager.getInstance().setCredentials("root", "tagada54");
	}
	
	@Test
	public void testAnonnymousAuthentication() throws OrtolangClientException, AuthenticationException {
		OrtolangClient client = new OrtolangClient();
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.UNAUTHENTIFIED_IDENTIFIER, profile);
		client.close();
	}
	
	@Test
	public void testRootAuthentication() throws OrtolangClientException, AuthenticationException {
		OrtolangClient client = new OrtolangClient();
		client.login("root");
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.SUPERUSER_IDENTIFIER, profile);
		client.close();
	}
	
	@Test
	public void testImportWorkspace() throws OrtolangClientException, AuthenticationException {
		OrtolangClient client = new OrtolangClient();
		client.login("root");
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.SUPERUSER_IDENTIFIER, profile);
		Map<String, String> params = new HashMap<String, String> ();
		params.put("wskey", "SLDR000745");
		params.put("wstype", "test");
		params.put("wsname", "SLDR 000 745");
		params.put("bagpath", "/media/space/jerome/Data/newbags/sldr000745");
		client.createProcess("import-workspace", "Import SLDR 745", params, Collections.<String, File> emptyMap());
		client.close();
	}

}
