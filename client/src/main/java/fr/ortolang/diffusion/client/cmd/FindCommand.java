package fr.ortolang.diffusion.client.cmd;

import javax.json.JsonObject;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class FindCommand extends Command {

	private Options options;
    private OrtolangClient client;

    public FindCommand() {
    	options = new Options();
        options.addOption("h", "help", false, "show help.");
        options.addOption("U", "username", true, "username for login");
        options.addOption("P", "password", true, "password for login");
        options.addOption("w", "workspace", true, "workspace key");
        options.addOption("r", "root", true, "root of the workspace (head or versionX)");
        options.addOption("f", "filter", true, "regex for filtering files");
    }

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        String wskey = null, root, path, filter = null;
        try {
			cmd = parser.parse(options, args);
	        if (cmd.hasOption("h")) {
	            help();
	        }
	        if (cmd.hasOption("w")) {
	        	wskey = cmd.getOptionValue("w");
            } else {
                help();
            }
	        if (cmd.hasOption("r")) {
	        	root = cmd.getOptionValue("r");
            } else {
                root = "head";
            }
	        if (cmd.hasOption("p")) {
	        	path = cmd.getOptionValue("p");
            } else {
            	path = "/";
            }
	        if (cmd.hasOption("f")) {
	        	filter = cmd.getOptionValue("f");
            }
	        
            String[] credentials = getCredentials(cmd);
            String username = credentials[0];
            String password = credentials[1];

            client = OrtolangClient.getInstance();
            if (username.length() > 0) {
                client.getAccountManager().setCredentials(username, password);
                client.login(username);
            }
            System.out.println("Connected as user: " + client.connectedProfile());
            System.out.println("Finds workspace (" + wskey +", " + root + ") element at " + path + " with filter : " + filter);
            walkFileTree(wskey, root, path, filter);
        } catch (ParseException e) {
            System.out.println("Failed to parse command line properties: " + e.getMessage());
            help();
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            System.out.println("Unexpected error !!");
            e.printStackTrace();
        }
	}
	
	private void walkFileTree(String wskey, String root, String path, String regex) throws OrtolangClientException, OrtolangClientAccountException {
		JsonObject wsElement = client.getWorkspaceElement(wskey, root, path);
		for (JsonObject elm : wsElement.getJsonArray("elements").getValuesAs(JsonObject.class)) {
			if (regex == null || (regex !=null && elm.getString("name").matches(regex))) {
				System.out.println(" - " + path + "/" + elm.getString("name"));
			}
			if (elm.getString("type").equals("collection")) {
				walkFileTree(wskey, root, path + (path != "/" ? "/" : "") + elm.getString("name"), regex);
			}
		}
	}

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Check Bag", options);
        System.exit(0);
    }

}
