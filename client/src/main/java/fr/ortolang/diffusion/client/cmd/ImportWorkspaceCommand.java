package fr.ortolang.diffusion.client.cmd;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ImportWorkspaceCommand extends Command {

    private final Options options = new Options();

    public ImportWorkspaceCommand() {
        options.addOption("h", "help", false, "show help.");
        options.addOption("U", "username", true, "username for login");
        options.addOption("P", "password", true, "password for login");
        options.addOption("f", "file", true, "bag file to import");
        options.addOption("upload", false, "bag file is local and need upload");
    }

    @Override
    public void execute(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        Map<String, String> params = new HashMap<>();
        Map<String, File> files = new HashMap<>();
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            }
            String[] credentials = getCredentials(cmd);
            String username = credentials[0];
            String password = credentials[1];

            if ( cmd.hasOption("f") ) {
                if (cmd.hasOption("upload")) {
                    files.put("bagpath", new File(cmd.getOptionValue("f")));
                } else {
                    params.put("bagpath", cmd.getOptionValue("f"));
                }
            } else {
                help();
            }

            OrtolangClient client = OrtolangClient.getInstance();
            if ( username.length() > 0 ) {
                client.getAccountManager().setCredentials(username, password);
                client.login(username);
            }
            System.out.println("Connected as user: " + client.connectedProfile());
            String pkey = client.createProcess("import-workspace", "Import Workspace '" + cmd.getOptionValue("f").substring(cmd.getOptionValue("f").lastIndexOf("/")+1) + "'", params, files);
            System.out.println("Import-Workspace process created with key : " + pkey);

            client.logout();
            client.close();

        } catch (ParseException e) {
            System.out.println("Failed to parse command line properties " +  e.getMessage());
            help();
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            System.out.println("Unexpected error !!");
            e.printStackTrace();
        }
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Import Workspace", options);
        System.exit(0);
    }

}
