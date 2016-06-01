package fr.ortolang.diffusion.client.cmd;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class ReindexCommand extends Command {

	private Options options = new Options();

	public ReindexCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
		options.addOption("s", "service", true, "service of the object to index");
		options.addOption("t", "type", true, "type of the object to index");
		options.addOption("F", "fake", false, "fake mode");
	}

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		String username = "";
		String password = null;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
			}
			if (cmd.hasOption("U")) {
				username = cmd.getOptionValue("U");
				if (cmd.hasOption("P")) {
					password = cmd.getOptionValue("P");
				} else {
					Console cons;
					char[] passwd;
					if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
					    password = new String(passwd);
					}
				}
			}
			
			String type = cmd.getOptionValue("t");
			String service = cmd.getOptionValue("s");
			boolean fakeMode = cmd.hasOption("F");
			
			if(type != null && service != null) {
				
				OrtolangClient client = OrtolangClient.getInstance();
				if ( username.length() > 0 ) {
					client.getAccountManager().setCredentials(username, password);
					client.login(username);
				}
				System.out.println("Connected as user: " + client.connectedProfile());
				System.out.println("Retrieving for published objects from service "+service+" and with type "+type+" ...");
				
				List<String> objectKeys = new ArrayList<String>();
				
				int offset = 0;
				int limit = 100;
				JsonObject listOfObjects = client.listObjects(service, type, "PUBLISHED", offset, limit);
				JsonArray keys = listOfObjects.getJsonArray("entries");
				
				while(!keys.isEmpty()) {
					for(JsonString objectKey : keys.getValuesAs(JsonString.class)) {
						objectKeys.add(objectKey.getString());
					}
					offset += limit;
					listOfObjects = client.listObjects(service, type, "PUBLISHED", offset, limit);
					keys = listOfObjects.getJsonArray("entries");
				}
				System.out.println("Reindex keys ("+objectKeys.size()+") : "+objectKeys);
				if(!fakeMode) {
					for(String key : objectKeys) {
						client.reindex(key);
					}
					System.out.println("All keys reindexed.");
				}
				client.logout();
				client.close();
			} else {
				help();
			}
		} catch (ParseException e) {
			System.out.println("Failed to parse comand line properties " +  e.getMessage());
			help();
		} catch (OrtolangClientException | OrtolangClientAccountException e) {
			System.out.println("Unexpected error !!");
			e.printStackTrace();
		}
	}

	private void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Reindex", options);
		System.exit(0);
	}

}
