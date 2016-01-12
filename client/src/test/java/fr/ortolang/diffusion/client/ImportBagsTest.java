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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class ImportBagsTest {

	private static Logger logger = Logger.getLogger(ImportBagsTest.class.getName());

	@Test
	public void importBags() throws IOException, OrtolangClientException, OrtolangClientAccountException {
		String bags = System.getProperty("bags.list");
		logger.log(Level.INFO, "Property bags.list : " + bags);
		if (bags == null) {
			bags = "";
		}

		OrtolangClient client = OrtolangClient.getInstance();
		client.getAccountManager().setCredentials("root", "tagada54");
		client.login("root");
		String profile = client.connectedProfile();
		logger.log(Level.INFO, "connected profile: {0}", profile);
		
		for (String bag : bags.split(",")) {
			logger.log(Level.INFO, "Create process to import bag: " + bag);
			String name = bag.substring(bag.lastIndexOf('/') + 1);
			Map<String, String> params = new HashMap<String, String>();
			params.put("bagpath", bag);
			client.createProcess("import-workspace", "Import " + name, params, Collections.<String, File> emptyMap());
		}

		client.logout();
		client.close();
	}
}
