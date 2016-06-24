package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
    private OrtolangClient client;

	public CopyCommand() {
		options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
        options.addOption("w", "workspace", true, "workspace alias targeted");
	}

	@Override
	public void execute(String[] args) {
		CommandLineParser parser = new BasicParser();
		CommandLine cmd;
		String username = "";
		String password = null;
        String localPath = null;
        String workspace = null;
        String remotePath = null;
        
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
			
			if (cmd.hasOption("w")) {
			    workspace = cmd.getOptionValue("w");
            } else {
                System.out.println("Workspace alias is needed (-w)");
                help();
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

    private void copy(Path localPath, String workspace, String remotePath) {
        try {
            Files.walkFileTree(localPath, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String remoteDir = remotePath + localPath.getParent().relativize(dir).toString();
                    System.out.println("Copying dir " + dir + " to " + workspace + ":" + remoteDir);
                    try {
                        client.writeCollection(workspace, remoteDir, "");
                    } catch (OrtolangClientException | OrtolangClientAccountException e) {
                        e.printStackTrace();
                        errors.append("-> Unable to copy dir ").append(dir).append(" to ").append(remoteDir).append("\r\n");
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String remoteDir = remotePath + localPath.getParent().relativize(file).toString();
                    System.out.println("Copying file " + file + " to " + workspace + ":" + remoteDir);
                    try {
                        client.writeDataObject(workspace, remoteDir, "", file.toFile(), null);
                    } catch (OrtolangClientException | OrtolangClientAccountException e) {
                        e.printStackTrace();
                        errors.append("-> Unable to copy file ").append(file).append(" to ").append(remoteDir).append("\r\n");
                        return FileVisitResult.TERMINATE;
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
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Copy local directory to an ortolang workspace", options);
		System.exit(0);
	}

}
