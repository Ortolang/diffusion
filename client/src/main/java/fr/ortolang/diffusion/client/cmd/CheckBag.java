package fr.ortolang.diffusion.client.cmd;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CheckBag extends Command {

    private Options options = new Options();

    public CheckBag() {
        options.addOption("h", "help", false, "show help.");
        options.addOption("p", "path", false, "path of the bag root");
    }

    @Override
    public void execute(String[] args) {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        String root = "";
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            }
            
            if (cmd.hasOption("p")) {
                root = cmd.getOptionValue("p");
            } else {
                help();
            }
            
            assert Files.exists(Paths.get(root)) : "Le chemin de base (" + root + ") n'existe pas";
            assert Files.exists(Paths.get(root, "publication.properties")) : "Le fichier publication.properties n'existe pas";
            assert Files.exists(Paths.get(root, "workspace.properties")) : "le fichier workspace.properties n'existe pas";
            
            if ( Files.exists(Paths.get(root, "snapshots")) ) {
                assert Files.isDirectory(Paths.get(root, "snapshots")) : "Snapshot n'est pas un dossier";
                Files.list(Paths.get(root, "snapshots")).forEach(this::checkSnapshot);
            }
            
            if ( Files.exists(Paths.get(root, "head")) ) {
                assert Files.isDirectory(Paths.get(root, "head")) : "Head n'est pas un dossier";
                checkSnapshot(Paths.get(root, "head"));
            }
            
            
        } catch (ParseException | IOException e) {
            System.err.println("Failed to parse command line properties: " + e.getMessage());
            help();
        } 
    }
    
    private void checkSnapshot(Path root) {
        Path metadata = Paths.get(root.toString(), "metadata");
        try {
            Files.walkFileTree(metadata, new FileVisitor<Path> () {
    
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = Paths.get(root.toString(), "objects", metadata.relativize(file).toString());
                    if ( !Files.exists(target)) {
                        System.out.println(("ERROR found metadata (" + file.toString() + ") that point to unexisting target file: " + target.toString()));
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
        formatter.printHelp("Check Bag", options);
        System.exit(0);
    }

}
