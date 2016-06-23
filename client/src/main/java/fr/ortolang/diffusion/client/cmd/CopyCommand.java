package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class CopyCommand extends Command {

	private Options options = new Options();
    private StringBuilder errors = new StringBuilder();

	public CopyCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
	}

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		String username = "";
		String password = null;
        String localPath = "";

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
			
//			if (cmd.hasOption("p")) {
//                localPath = cmd.getOptionValue("p");
//            } else {
//                help();
//            }
			List<String> argList = cmd.getArgList();
			if (argList.size() < 2) {
				System.out.println(argList);
				System.out.println("No argument");
				help();
			} else {
				localPath = argList.get(0);
			}
			
			OrtolangClient client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			if ( !Files.exists(Paths.get(localPath)) ) {
                errors.append("-> Le chemin local (").append(localPath).append(") n'existe pas\r\n");
            } else {
                
//                if (Files.exists(Paths.get(localPath, "data", "snapshots"))) {
//                    Files.list(Paths.get(localPath, "data", "snapshots")).forEach(this::checkSnapshotMetadata);
//                    Files.list(Paths.get(localPath, "data", "snapshots")).forEach(this::checkPermissions);
//                }
//
//                if (Files.exists(Paths.get(localPath, "data", "head"))) {
//                    checkSnapshotMetadata(Paths.get(localPath, "data", "head"));
//                }
            }
            if (errors.length() > 0) {
                System.out.println("## Some errors has been found : ");
                System.out.print(errors.toString());
            } else {
                System.out.println("No error found.");
            }
//			client.writeCollection(workspace, path, description);
			
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
		formater.printHelp("Copy local directory to an ortolang workspace", options);
		System.exit(0);
	}

}
