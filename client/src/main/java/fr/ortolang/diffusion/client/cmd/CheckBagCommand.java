package fr.ortolang.diffusion.client.cmd;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonReader;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

public class CheckBagCommand extends Command {

    private Options options = new Options();
    private StringBuilder errors = new StringBuilder();
    private StringBuilder fixed = new StringBuilder();
    private boolean fix = false;

    public CheckBagCommand() {
        options.addOption("h", "help", false, "show help.");
        options.addOption("p", "path", true, "path of the bag root");
        options.addOption("f", "fix", false, "fix problems (WARNING may delete some files)");
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

            if (cmd.hasOption("f")) {
                fix = true;
            }

            if (cmd.hasOption("p")) {
                root = cmd.getOptionValue("p");
            } else {
                help();
            }

            if (!Files.exists(Paths.get(root))) {
                errors.append("-> Le chemin de base (").append(root).append(") n'existe pas\r\n");
            } else {
                if (!Files.exists(Paths.get(root, "data", "publication.properties"))) {
                    errors.append("-> publication.properties NOT found\r\n");
                }
                if (!Files.exists(Paths.get(root, "data", "workspace.properties"))) {
                    errors.append("-> workspace.properties NOT found\r\n");
                }

                if (Files.exists(Paths.get(root, "data", "snapshots"))) {
                    Files.list(Paths.get(root, "data", "snapshots")).forEach(this::checkSnapshotMetadata);
                    Files.list(Paths.get(root, "data", "snapshots")).forEach(this::checkPermissions);
                }

                if (Files.exists(Paths.get(root, "data", "head"))) {
                    checkSnapshotMetadata(Paths.get(root, "data", "head"));
                }
            }
            if (errors.length() > 0) {
                System.out.println("## Some errors has been found : ");
                System.out.print(errors.toString());
                if (fix) {
                    System.out.println("## Some errors has been fixed : ");
                    System.out.print(fixed.toString());
                }
            } else {
                System.out.println("No error found.");
            }

        } catch (ParseException | IOException e) {
            System.out.println("Failed to parse command line properties: " + e.getMessage());
            help();
        }
    }

    private void checkSnapshotMetadata(Path root) {
        Path metadata = Paths.get(root.toString(), "metadata");
        try {
            Files.walkFileTree(metadata, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = Paths.get(root.toString(), "objects", metadata.relativize(file.getParent()).toString());
                    if (!Files.exists(target)) {
                        errors.append("-> unexisting target for metadata: ").append(file).append("\r\n");
                        if (fix) {
                            try {
                                Files.delete(file);
                                fixed.append("-> deleted metadata: ").append(file).append("\r\n");
                            } catch (IOException e) {
                                errors.append("-> unable to fix: ").append(e.getMessage()).append("\r\n");
                            }
                        }
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
    
    private void checkPermissions(Path root) {
        Path metadata = Paths.get(root.toString(), "metadata");
        Path objects = Paths.get(root.toString(), "objects");
        try {
            checkPathPermissions(objects, metadata, -1, new HashSet<Path>());
        } catch (IOException e) {
            System.out.println("Unable to walk file tree: " + e.getMessage());
        }
    }
    
    private void checkPathPermissions(Path node, Path nodeMD, int parentLevel, Set<Path> treatedNodes) throws IOException {
        int nodeLevel = parentLevel;
        if ( !treatedNodes.contains(node) ) {
            if (Files.exists(nodeMD)) {
                Path permissionMD = Paths.get(nodeMD.toString(), "ortolang-acl-json");
                if (Files.exists(permissionMD) && !Files.isDirectory(permissionMD)) {
                    nodeLevel = parseACLLevel(permissionMD);
                    if ( nodeLevel < parentLevel ) {
                        errors.append("-> unconsistent file acl permission for object: ").append(node).append("\r\n");
                        if (fix) {
                            Path aclParent = Paths.get(nodeMD.getParent().toString(), "ortolang-acl-json");
                            try (OutputStream os = Files.newOutputStream(aclParent)) {
                                Template tpl = Template.findTemplateByLevel(nodeLevel);
                                IOUtils.write(tpl.getJson(), os);
                                os.flush();
                                fixed.append("-> new acl [" + tpl.getName()  + "] set for parent of object: ").append(node).append("\r\n");
                            }
                        }
                        for ( String sibling : node.getParent().toFile().list() ) {
                            if ( !sibling.equals(node.toString()) ) {
                                Path aclSibling = Paths.get(sibling, "ortolang-acl-json");
                                if ( !Files.exists(aclSibling)) {
                                    if (fix) {
                                        try (OutputStream os = Files.newOutputStream(aclSibling)) {
                                            Template tpl = Template.findTemplateByLevel(nodeLevel);
                                            IOUtils.write(tpl.getJson(), os);
                                            os.flush();
                                            fixed.append("-> new acl [" + tpl.getName()  + "] set for sibling of object: ").append(node).append("\r\n");
                                        }
                                    }
                                    treatedNodes.add(node);
                                }
                            }
                        }
                    }
                }
            }
        }
        treatedNodes.add(node);
        if ( Files.isDirectory(node) ) {
            for ( String child : node.toFile().list() ) {
                checkPathPermissions(Paths.get(node.toString(), child), Paths.get(nodeMD.toString(), child), ((nodeLevel < 0)?0:nodeLevel), treatedNodes);
            }
        }
    }

    private int parseACLLevel(Path aclFile) throws IOException {
        JsonReader reader = Json.createReader(Files.newInputStream(aclFile));
        String name = reader.readObject().getString("template");
        return Template.findTemplateByName(name).getLevel();
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Check Bag", options);
        System.exit(0);
    }

    public static class Template {
        
        static Map<String, Template> templates = new HashMap<String, Template>();
        static {
            templates.put("forall", new Template(0, "forall", "{\"template\":\"forall\"}"));
            templates.put("authentified", new Template(1, "authentified", "{\"template\":\"authentified\"}"));
            templates.put("esr", new Template(2, "esr", "{\"template\":\"esr\"}"));
            templates.put("restricted", new Template(3, "restricted", "{\"template\":\"restricted\"}"));
        }

        private int level;
        private String name;
        private String json;

        private Template(int level, String name, String json) {
            this.level = level;
            this.name = name;
            this.json = json;
        }

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }

        public String getJson() {
            return json;
        }
        
        public static Template findTemplateByName(String name) {
            return templates.get(name);
        }
        
        public static Template findTemplateByLevel(int level) {
            for (Template template : templates.values()) {
                if ( template.getLevel() == level ) {
                    return template;
                }
            }
            return templates.get("forall");
        }
        
    }

}
