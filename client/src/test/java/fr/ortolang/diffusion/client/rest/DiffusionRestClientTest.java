package fr.ortolang.diffusion.client.rest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.iharder.Base64;

import org.junit.Test;

import fr.ortolang.diffusion.client.rest.OrtolangRestClient;
import fr.ortolang.diffusion.client.rest.OrtolangRestClientException;
import fr.ortolang.diffusion.membership.MembershipService;

public class DiffusionRestClientTest {
	
	private static Logger logger = Logger.getLogger(DiffusionRestClientTest.class.getName());
	
	@Test
	public void testAuthentication() throws OrtolangRestClientException {
		OrtolangRestClient client = new OrtolangRestClient();
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.UNAUTHENTIFIED_IDENTIFIER, profile);
	}
	
	@Test
	public void testAuthenticationLogged() throws OrtolangRestClientException {
		OrtolangRestClient client = new OrtolangRestClient();
		String credentials = MembershipService.SUPERUSER_IDENTIFIER + ":tagada54";
		String authorisation = "Basic " + Base64.encodeBytes(credentials.getBytes());
		client.setAutorisationHeader(authorisation);
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.SUPERUSER_IDENTIFIER, profile);
	}
	
	@Test
	public void testImportWorkspace() throws OrtolangRestClientException {
		OrtolangRestClient client = new OrtolangRestClient();
		String credentials = MembershipService.SUPERUSER_IDENTIFIER + ":tagada54";
		String authorisation = "Basic " + Base64.encodeBytes(credentials.getBytes());
		client.setAutorisationHeader(authorisation);
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.SUPERUSER_IDENTIFIER, profile);
		Map<String, String> params = new HashMap<String, String> ();
		params.put("wskey", "SLDR000745");
		params.put("wstype", "test");
		params.put("wsname", "SLDR 000 745");
		params.put("bagpath", "/media/space/jerome/Data/newbags/sldr000745");
		
		client.createProcess("import-workspace", "Import SLDR 745", params, Collections.<String, File> emptyMap());
	}

}
