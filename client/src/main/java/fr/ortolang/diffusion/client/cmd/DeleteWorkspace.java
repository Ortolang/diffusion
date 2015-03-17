package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

public class DeleteWorkspace {

	private static final Logger log = Logger.getLogger(DeleteWorkspace.class.getName());
	private String[] args = null;
	private Options options = new Options();

	public DeleteWorkspace(String[] args) {
		this.args = args;
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
		options.addOption("k", "key", true, "the workspace key");
	}

	public void parse() {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		String username = "";
		String password = "";
		Map<String, String> params = new HashMap<String, String> ();
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
			}
			if (cmd.hasOption("u")) {
				log.log(Level.INFO, "Using provided username: " + cmd.getOptionValue("u"));
				username = cmd.getOptionValue("u");
				if (cmd.hasOption("p")) {
					log.log(Level.INFO, "Using provided password");
				} else {
					Console cons;
					char[] passwd;
					if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
					    java.util.Arrays.fill(passwd, ' ');
					    password = new String(passwd);
					}
				}
			}
			
			if (cmd.hasOption("k")) {
				params.put("wskey", cmd.getOptionValue("k"));
			} else {
				help();
			}

			OrtolangClient client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			String pkey = client.createProcess("delete-workspace", "DeleteWorkspace", params, Collections.<String, File> emptyMap());
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
		formater.printHelp("Delete Workspace", options);
		System.exit(0);
	}

	public static void main(String[] args) {
		new ImportWorkspace(args).parse();;
	}

}
