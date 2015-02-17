package fr.ortolang.diffusion.client;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountManager;
import fr.ortolang.diffusion.membership.MembershipService;

public class OrtolangClientTest {
	
	private static Logger logger = Logger.getLogger(OrtolangClientTest.class.getName());
	private static String clientId = "client";
	
	@BeforeClass
	public static void init() throws OrtolangClientAccountException {
		OrtolangClientAccountManager.getInstance(clientId).setCredentials("root", "tagada54");
	}
	
	@Test
	public void testAnonnymousAuthentication() throws OrtolangClientException {
		OrtolangClient client = new OrtolangClient(clientId);
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.UNAUTHENTIFIED_IDENTIFIER, profile);
		client.close();
	}
	
	@Test
	public void testRootAuthentication() throws OrtolangClientException {
		OrtolangClient client = new OrtolangClient(clientId);
		client.login("root");
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		assertEquals(MembershipService.SUPERUSER_IDENTIFIER, profile);
		client.close();
	}
	
	@Test
	public void testImportWorkspace() throws OrtolangClientException {
		OrtolangClient client = new OrtolangClient(clientId);
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
