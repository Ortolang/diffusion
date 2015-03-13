package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class ImportWorkspace {

	private static final Logger log = Logger.getLogger(ImportWorkspace.class.getName());
	private String[] args = null;
	private Options options = new Options();

	public ImportWorkspace(String[] args) {
		this.args = args;
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
		options.addOption("a", "alias", true, "a unique alias for this workspace");
		options.addOption("n", "name", true, "a friendly name for the workspace");
		options.addOption("t", "type", true, "default type is 'user'");
		options.addOption("f", "file", true, "bag file to import");
		options.addOption("p", "path", true, "bag path to import (server side)");
	}

	public void parse() {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		String username = "";
		String password = "";
		Map<String, String> params = new HashMap<String, String> ();
		Map<String, File> files = new HashMap<String, File> ();
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
			}
			if (cmd.hasOption("U")) {
				log.log(Level.INFO, "Using provided username: " + cmd.getOptionValue("U"));
				username = cmd.getOptionValue("U");
				if (cmd.hasOption("P")) {
					log.log(Level.INFO, "Using provided password");
				} else {
					Console cons;
					char[] passwd;
					if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
					    password = new String(passwd);
					}
				}
			}
			
			params.put("wskey", UUID.randomUUID().toString());
			if (cmd.hasOption("f")) {
				files.put("bagpath", new File(cmd.getOptionValue("f")));
			} else if (cmd.hasOption("p")) {
				params.put("bagpath", cmd.getOptionValue("p"));
			} else {
				help();
			}
			if (cmd.hasOption("a")) {
				params.put("wsalias", cmd.getOptionValue("a"));
			} 
			if (cmd.hasOption("n")) {
				params.put("wsname", cmd.getOptionValue("n"));
			}
			if (cmd.hasOption("t")) {
				params.put("wstype", cmd.getOptionValue("t"));
			} else {
				params.put("wstype", "user");
			}

			OrtolangClient client = new OrtolangClient();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			String pkey = client.createProcess("import-workspace", "ImportWorkspace", params, files);
			log.log(Level.INFO, "Process created with key : " + pkey);
			
			client.logout();
			client.close();
			
		} catch (ParseException e) {
			log.log(Level.SEVERE, "Failed to parse comand line properties", e);
			help();
		} catch (OrtolangClientException | OrtolangClientAccountException e) {
			log.log(Level.SEVERE, "Unexpected error", e);
			System.out.println(e.getMessage());
		}
	}

	private void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Import Workspace", options);
		System.exit(0);
	}

	public static void main(String[] args) {
		new ImportWorkspace(args).parse();;
	}

}
