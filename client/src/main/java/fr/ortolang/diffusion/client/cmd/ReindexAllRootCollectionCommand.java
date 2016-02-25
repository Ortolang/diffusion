package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class ReindexAllRootCollectionCommand extends Command {

	private Options options = new Options();

	public ReindexAllRootCollectionCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
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

			boolean fakeMode = cmd.hasOption("F");
			
			OrtolangClient client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			System.out.println("Looking for root collection ...");
			
			// Looking for root collection
			List<String> rootCollectionKeys = new ArrayList<String>();
			
			int offset = 0;
			int limit = 100;
			JsonObject listOfObjects = client.listObjects("core", "collection", "PUBLISHED", offset, limit);
			JsonArray keys = listOfObjects.getJsonArray("entries");
			
			while(!keys.isEmpty()) {
				for(JsonString objectKey : keys.getValuesAs(JsonString.class)) {
					JsonObject objectRepresentation = client.getObject(objectKey.getString());
					JsonObject objectProperty = objectRepresentation.getJsonObject("object");
					boolean isRoot = objectProperty.getBoolean("root");
					if(isRoot) {
						rootCollectionKeys.add(objectKey.getString());
					}
				}
				offset += limit;
				listOfObjects = client.listObjects("core", "collection", "PUBLISHED", offset, limit);
				keys = listOfObjects.getJsonArray("entries");
			}
			
			System.out.println("Reindex keys : "+rootCollectionKeys);
			if(!fakeMode) {
				for(String key : rootCollectionKeys) {
					client.reindex(key);
				}
			}
			
			client.logout();
			client.close();
			
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
		formater.printHelp("Index Workspace", options);
		System.exit(0);
	}

}
