package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.File;
import java.util.Collections;
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

public class DeleteWorkspaceCommand extends Command {

    private Options options = new Options();

    public DeleteWorkspaceCommand() {
        options.addOption("h", "help", false, "show help.");
        options.addOption("U", "username", true, "username for login");
        options.addOption("P", "password", true, "password for login");
        options.addOption("k", "key", true, "the workspace key");
        options.addOption("a", "alias", true, "the workspace alias");
        options.addOption("f", "force", false, "force the supression even if workspace contains published versions (must be root)");
    }

    @Override
    public void execute(String[] args) {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        String username = "";
        String password = null;
        Map<String, String> params = new HashMap<String, String> ();
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

            if (cmd.hasOption("k")) {
                params.put("wskey", cmd.getOptionValue("k"));
            } else if (cmd.hasOption("a")) {
                params.put("wsalias", cmd.getOptionValue("a"));
            } else {
                help();
            }
            
            if (cmd.hasOption("f")) {
                params.put("force", "true");
            }

            OrtolangClient client = OrtolangClient.getInstance();
            if ( username.length() > 0 ) {
                client.getAccountManager().setCredentials(username, password);
                client.login(username);
            }
            System.out.println("Connected as user: " + client.connectedProfile());
            String pkey = client.createProcess("delete-workspace", "DeleteWorkspace", params, Collections.<String, File> emptyMap());
            System.out.println("Delete-Workspace process created with key : " + pkey);

            client.logout();
            client.close();

        } catch (ParseException e) {
            System.out.println("Failed to parse command line properties: " + e.getMessage());
            help();
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            System.out.println("Unexpected error !!");
            e.printStackTrace();
        }
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Delete Workspace", options);
        System.exit(0);
    }

}
