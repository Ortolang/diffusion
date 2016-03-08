package fr.ortolang.diffusion.client.cmd;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

public class CheckBagCommand extends Command {

    public static final Pattern ORTOLANG_KEY_MATCHER = Pattern.compile("\\$\\{([\\w\\d:\\-_]*)\\}");

    private Options options = new Options();
    private StringBuilder errors = new StringBuilder();
    private StringBuilder fixed = new StringBuilder();
    private boolean fix = false;
    private OrtolangClient client;

    public CheckBagCommand() {
        options.addOption("h", "help", false, "show help.");
		options.addOption("U", "username", true, "username for login");
		options.addOption("P", "password", true, "password for login");
        options.addOption("p", "path", true, "path of the bag root");
        options.addOption("f", "fix", false, "fix problems (WARNING may delete some files)");
    }

    @Override
    public void execute(String[] args) {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        String root = "";
		String username = "";
		String password = null;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            }
            
            if (cmd.hasOption("f")) {
                fix = true;
            }
            
            if (cmd.hasOption("p")) {
                root = cmd.getOptionValue("p");
            } else {
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
			client = OrtolangClient.getInstance();
			if ( username.length() > 0 ) {
				client.getAccountManager().setCredentials(username, password);
				client.login(username);
			}
			System.out.println("Connected as user: " + client.connectedProfile());
			
            if ( !Files.exists(Paths.get(root)) ) {
                errors.append("-> Le chemin de base (").append(root).append(") n'existe pas\r\n");
            } else {
                if ( !Files.exists(Paths.get(root, "data", "publication.properties")) ) {
                    errors.append("-> publication.properties NOT found\r\n");
                }
                if ( !Files.exists(Paths.get(root, "data", "workspace.properties")) ) {
                    errors.append("-> workspace.properties NOT found\r\n");
                } else {
                	checkWorkspaceProperties(Paths.get(root, "data", "workspace.properties"));
                }
                
                if ( Files.exists(Paths.get(root, "data", "snapshots")) ) {
                    Files.list(Paths.get(root, "data", "snapshots")).forEach(this::checkSnapshotMetadata);
                }
                
                if ( Files.exists(Paths.get(root, "data", "head")) ) {
                    checkSnapshotMetadata(Paths.get(root, "data", "head"));
                }
            }
            if ( errors.length() > 0 ) {
                System.out.println("## Some errors has been found : ");
                System.out.print(errors.toString());
                if ( fix ) {
                    System.out.println("## Some errors has been fixed : ");
                    System.out.print(fixed.toString());
                }
            } else {
                System.out.println("No error found.");
            }
            
        } catch (ParseException | IOException e) {
            System.out.println("Failed to parse command line properties: " + e.getMessage());
            help();
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
			System.out.println("Unexpected error !!");
			e.printStackTrace();
		}
    }
    
    private void checkSnapshotMetadata(Path root) {
        Path metadata = Paths.get(root.toString(), "metadata");
        try {
            Files.walkFileTree(metadata, new FileVisitor<Path> () {
    
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = Paths.get(root.toString(), "objects", metadata.relativize(file.getParent()).toString());
                    if ( !Files.exists(target) ) {
                        errors.append("-> unexisting target for metadata: ").append(file).append("\r\n");
                        if ( fix ) {
                            try {
                                Files.delete(file);
                                fixed.append("-> deleted metadata: ").append(file).append("\r\n");
                            } catch ( IOException e ) {
                                errors.append("-> unable to fix: ").append(e.getMessage()).append("\r\n");
                            }
                        }
                    } else if ( file.endsWith("ortolang-item-json") ) {
                        checkOrtolangItemJson(file);
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

    private void checkWorkspaceProperties(Path workspaceFilePath) throws IOException, OrtolangClientException {
        Properties props = new Properties();
        InputStream in = Files.newInputStream(workspaceFilePath);
        props.load(in);
        //TODO check whether workspace alias is availabled
        String owner = props.getProperty("owner");
        if(owner!=null) {
        	checkObject(owner, "owner");
        }
        String members = props.getProperty("members");
        if(members!=null) {
        	for(String member : members.split(",")) {
        		checkObject(member, "member");
        	}
        }
    }
    
    private void checkOrtolangItemJson(Path filepath) {
        String jsonContent = getContent(filepath);
        if(jsonContent!=null) {
            List<String> keys = extractOrtolangKeys(jsonContent);
            
            System.out.println("Looking for keys in registry : "+keys);
            keys.parallelStream().forEach((key) -> checkObject(key, "referential") );
        }
    }
    
    private void checkObject(String key, String subject) {
		try {
			JsonObject object = client.getObject(key);
			if(object==null) {
				errors.append("-> ").append(subject).append(" ").append(key).append(" doesn't exist\r\n");
			}
		} catch (OrtolangClientException | OrtolangClientAccountException e) {
			errors.append("-> unable to find ").append(key).append(" : ").append(e.getMessage()).append("\r\n");
		}
    }

    private String getContent(Path filepath) {
        String content = null;
        try ( InputStream is = Files.newInputStream(filepath) ) {
            content = IOUtils.toString(is);
        } catch (IOException e) {
            System.out.println("  unable to get content of file : "+filepath+" : "+e.getMessage());
        }
        return content;
    }

    public static List<String> extractOrtolangKeys(String json) {
        Matcher okMatcher = CheckBagCommand.ORTOLANG_KEY_MATCHER.matcher(json);
        List<String> ortolangKeys = new ArrayList<String>();
        while(okMatcher.find()) {
            if(!ortolangKeys.contains(okMatcher.group(1))) {
                ortolangKeys.add(okMatcher.group(1));
            }
        }
        return ortolangKeys;
    }
    
    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Check Bag", options);
        System.exit(0);
    }

}
