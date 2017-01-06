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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.cli.*;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class CopyCommand extends Command {
	
	private Options options = new Options();
    private StringBuilder errors = new StringBuilder();
    private OrtolangClient client;
    private String mode;

	public CopyCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
        options.addOption("w", "workspace", true, "workspace key targeted");
        options.addOption("m", "mode", true, "mode");
        
        mode = "objects";
	}

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
        String localPath = null;
        String workspace = null;
        String remotePath = null;
        
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				help();
			}
            String[] credentials = getCredentials(cmd);
            String username = credentials[0];
            String password = credentials[1];
			
			if (cmd.hasOption("w")) {
			    workspace = cmd.getOptionValue("w");
            } else {
                System.out.println("Workspace key is needed (-w)");
                help();
            }
			
			if (cmd.hasOption("m")) {
				//TODO validate (with an enum ?)
			    mode = cmd.getOptionValue("m");
            }
			
			List<String> argList = cmd.getArgList();
			if (argList.size() < 2) {
				System.out.println("Two arguments is needed (localpath and remotepath)");
				help();
			} else {
				localPath = argList.get(0);
                remotePath = argList.get(1);
			}
			
			client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			if ( !Files.exists(Paths.get(localPath)) ) {
                errors.append("-> Le chemin local (").append(localPath).append(") n'existe pas\r\n");
            } else {
                //TODO Checks if remote Path exist
                if (Files.exists(Paths.get(localPath))) {
                  copy(Paths.get(localPath), workspace, remotePath);
              }
            }
            if (errors.length() > 0) {
                System.out.println("## Some errors has been found : ");
                System.out.print(errors.toString());
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

    private void copy(Path localPath, String workspace, String remotePath) {
        try {
            Files.walkFileTree(localPath, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                	switch(mode) {
                	case "objects":
                		 String remoteDir = remotePath + localPath.getParent().relativize(dir).toString();
 	                    System.out.println("Copying dir " + dir + " to " + workspace + ":" + remoteDir);
 	                    try {
 	                        client.writeCollection(workspace, remoteDir, "");
 	                    } catch (OrtolangClientException | OrtolangClientAccountException e) {
 	                        e.printStackTrace();
 	                        errors.append("-> Unable to copy dir ").append(dir).append(" to ").append(remoteDir).append("\r\n");
 	                        return FileVisitResult.TERMINATE;
 	                    }
                	}
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                	switch(mode) {
                	case "objects":
                		String remoteFile = remotePath + localPath.getParent().relativize(file).toString();
	                    System.out.println("Copying file " + file + " to " + workspace + ":" + remoteFile);
	                    try {
	                        client.writeDataObject(workspace, remoteFile, "", file.toFile(), null);
	                    } catch (OrtolangClientException | OrtolangClientAccountException e) {
	                        e.printStackTrace();
	                        errors.append("-> Unable to copy file ").append(file).append(" to ").append(remoteFile).append("\r\n");
	                        return FileVisitResult.TERMINATE;
	                    }
	                    break;
                	case "metadata": 
                		String remoteDir = remotePath + localPath.getParent().relativize(file).getParent().toString();
	                    System.out.println("Creating metadata file " + file + " to " + workspace + ":" + remoteDir);
                		String name = file.getFileName().toString();
                		try {
							client.writeMetaData(workspace, remoteDir, name, null, file.toFile());
						} catch (OrtolangClientException | OrtolangClientAccountException e) {
							 e.printStackTrace();
	                        errors.append("-> Unable to copy file ").append(file).append(" to ").append(remoteDir).append("\r\n");
	                        return FileVisitResult.TERMINATE;
						}
                		break;
                	}
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            System.out.println("Unable to walk file tree: " + e.getMessage());
        }
    }
    
	private void help() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("Copy local directory to an ortolang workspace", options);
		System.exit(0);
	}

}
