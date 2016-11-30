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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OrtolangCli {

    private static OrtolangCli instance;
    private final Map<String, String> commands = new HashMap<>();

    private OrtolangCli() {
        commands.put("delete-workspace", DeleteWorkspaceCommand.class.getName());
        commands.put("import-workspace", ImportWorkspaceCommand.class.getName());
        commands.put("publish-workspace", PublishWorkspaceCommand.class.getName());
        commands.put("import-zip", ImportZipCommand.class.getName());
        commands.put("import-profiles", ImportProfilesCommand.class.getName());
        commands.put("import-referential", ImportReferentialCommand.class.getName());
        commands.put("check-bag", CheckBagCommand.class.getName());
        commands.put("reindex-all-root-collection", ReindexAllRootCollectionCommand.class.getName());
        commands.put("reindex", ReindexCommand.class.getName());
        commands.put("index-all", IndexAllCommand.class.getName());
        commands.put("cp", CopyCommand.class.getName());
    }

    public static OrtolangCli getInstance() {
        if (instance == null) {
            instance = new OrtolangCli();
        }
        return instance;
    }

    public void parse(String[] args) {
        if ( args.length > 0 ) {
            String commandName = args[0];
            if ( commands.containsKey(commandName) ) {
                System.out.println("Executing command: " + commandName);
                try {
                    Command command = (Command) Class.forName(commands.get(commandName)).newInstance();
                    command.execute(Arrays.copyOfRange(args, 1, args.length));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Command not found");
                help();
            }
        } else {
            help();
        }
    }

    private void help() {
        System.out.println("Ortolang CLI available commands :");
        for (String command : commands.keySet()) {
            System.out.println("\t " + command);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        OrtolangCli.getInstance().parse(args);
    }

}
