package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class ImportZipCommand extends Command {

	private Options options = new Options();

	public ImportZipCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
		options.addOption("k", "key", true, "the workspace key");
		options.addOption("p", "path", true, "the collection's path for import");
		options.addOption("f", "file", true, "bag file to import");
		options.addOption("upload", false, "bag file is local and need upload");
		options.addOption("overwrite", false, "overwrite existing files");
	}

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		String username = "";
		String password = null;
		Map<String, String> params = new HashMap<String, String> ();
		Map<String, File> files = new HashMap<String, File> ();
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
			
			if ( cmd.hasOption("f") ) {
				if (cmd.hasOption("upload")) {
					files.put("zippath", new File(cmd.getOptionValue("f")));
				} else {
					params.put("zippath", cmd.getOptionValue("f"));
				}
			} else {
				help();
			}
			
			if ( cmd.hasOption("k") ) {
				params.put("wskey", cmd.getOptionValue("k"));
			} else {
				help();
			}
			
			if ( cmd.hasOption("p") ) {
				params.put("ziproot", cmd.getOptionValue("p"));
			} else {
				help();
			}
			
			if ( cmd.hasOption("overwrite") ) {
				params.put("overwrite", "true");
			} 
			
			OrtolangClient client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			String pkey = client.createProcess("import-zip", "Import Zip", params, files);
			System.out.println("Import-Zip process created with key : " + pkey);
			
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
		formater.printHelp("Import Zip", options);
		System.exit(0);
	}

}
