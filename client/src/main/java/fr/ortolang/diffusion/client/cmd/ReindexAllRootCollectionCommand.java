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

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import org.apache.commons.cli.*;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.ArrayList;
import java.util.List;

public class ReindexAllRootCollectionCommand extends Command {

    private final Options options = new Options();

    public ReindexAllRootCollectionCommand() {
        options.addOption("h", "help", false, "show help.");
        options.addOption("U", "username", true, "username for login");
        options.addOption("P", "password", true, "password for login");
        options.addOption("F", "fake", false, "fake mode");
    }

    @Override
    public void execute(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            }

            String[] credentials = getCredentials(cmd);
            String username = credentials[0];
            String password = credentials[1];

            boolean fakeMode = cmd.hasOption("F");

            OrtolangClient client = OrtolangClient.getInstance();
            if ( username.length() > 0 ) {
                client.getAccountManager().setCredentials(username, password);
                client.login(username);
            }
            System.out.println("Connected as user: " + client.connectedProfile());
            System.out.println("Looking for root collection ...");

            // Looking for root collection
            List<String> rootCollectionKeys = new ArrayList<>();

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
            System.out.println("Failed to parse command line properties " +  e.getMessage());
            help();
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            System.out.println("Unexpected error !!");
            e.printStackTrace();
        }
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Index all root collection", options);
        System.exit(0);
    }

}
