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
import org.apache.commons.io.IOUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ALL") public class CheckBagCommand extends Command {

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
        CommandLineParser parser = new DefaultParser();
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

            String[] credentials = getCredentials(cmd);
            String username = credentials[0];
            String password = credentials[1];

            client = OrtolangClient.getInstance();
            if (username.length() > 0) {
                client.getAccountManager().setCredentials(username, password);
                client.login(username);
            }
            System.out.println("Connected as user: " + client.connectedProfile());

            if (!Files.exists(Paths.get(root))) {
                errors.append("-> Le chemin de base (").append(root).append(") n'existe pas\r\n");
            } else {
                if (!Files.exists(Paths.get(root, "data", "publication.properties"))) {
                    errors.append("-> publication.properties NOT found\r\n");
                }
                if (!Files.exists(Paths.get(root, "data", "workspace.properties"))) {
                    errors.append("-> workspace.properties NOT found\r\n");
                } else {
                    checkWorkspaceProperties(Paths.get(root, "data", "workspace.properties"));
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
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            System.out.println("Unexpected error !!");
            e.printStackTrace();
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
                    } else if (file.endsWith("ortolang-item-json")) {
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

    private void checkPermissions(Path root) {
        Path metadata = Paths.get(root.toString(), "metadata");
        Path objects = Paths.get(root.toString(), "objects");
        try {
            checkPathPermissions(objects, metadata, -1, new HashSet<>());
        } catch (IOException e) {
            System.out.println("Unable to walk file tree: " + e.getMessage());
        }
    }

    private void checkPathPermissions(Path node, Path nodeMD, int parentLevel, Set<Path> treatedNodes) throws IOException {
        int nodeLevel = parentLevel;
        if (!treatedNodes.contains(node)) {
            if (Files.exists(nodeMD)) {
                Path permissionMD = Paths.get(nodeMD.toString(), "ortolang-acl-json");
                if (Files.exists(permissionMD) && !Files.isDirectory(permissionMD)) {
                    nodeLevel = parseACLLevel(permissionMD);
                    if (nodeLevel < parentLevel) {
                        errors.append("-> unconsistent file acl permission for object: ").append(node).append("\r\n");
                        if (fix) {
                            Path aclParent = Paths.get(nodeMD.getParent().toString(), "ortolang-acl-json");
                            try (OutputStream os = Files.newOutputStream(aclParent)) {
                                Template tpl = Template.findTemplateByLevel(nodeLevel);
                                IOUtils.write(tpl.getJson(), os, "UTF-8");
                                os.flush();
                                //noinspection StringConcatenationInsideStringBufferAppend
                                fixed.append("-> new acl [").append(tpl.getName()).append("] set for parent of object: ").append(node).append("\r\n");
                            }
                        }
                        for (String sibling : node.getParent().toFile().list()) {
                            if (!sibling.equals(node.toString())) {
                                Path aclSibling = Paths.get(sibling, "ortolang-acl-json");
                                if (!Files.exists(aclSibling)) {
                                    if (fix) {
                                        try (OutputStream os = Files.newOutputStream(aclSibling)) {
                                            Template tpl = Template.findTemplateByLevel(nodeLevel);
                                            IOUtils.write(tpl.getJson(), os, "UTF-8");
                                            os.flush();
                                            //noinspection StringConcatenationInsideStringBufferAppend
                                            fixed.append("-> new acl [").append(tpl.getName()).append("] set for sibling of object: ").append(node).append("\r\n");
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
        if (Files.isDirectory(node)) {
            for (String child : node.toFile().list()) {
                checkPathPermissions(Paths.get(node.toString(), child), Paths.get(nodeMD.toString(), child), (nodeLevel < 0 ? 0 : nodeLevel), treatedNodes);
            }
        }
    }

    private int parseACLLevel(Path aclFile) throws IOException {
        JsonReader reader = Json.createReader(Files.newInputStream(aclFile));
        String name = reader.readObject().getString("template");
        return Template.findTemplateByName(name).getLevel();
    }

    private void checkWorkspaceProperties(Path workspaceFilePath) throws IOException {
        Properties props = new Properties();
        InputStream in = Files.newInputStream(workspaceFilePath);
        props.load(in);
        //TODO check whether workspace alias is availabled
        String owner = props.getProperty("owner");
        if (owner != null) {
            checkObject(owner, "owner");
        }
        String members = props.getProperty("members");
        if (members != null) {
            for (String member : members.split(",")) {
                checkObject(member, "member");
            }
        }
    }

    private void checkOrtolangItemJson(Path filepath) {
        String jsonContent = getContent(filepath);
        if (jsonContent != null) {
            List<String> keys = extractOrtolangKeys(jsonContent);

            System.out.println("Looking for keys in registry : " + keys);
            keys.parallelStream().forEach((key) -> checkObject(key, "referential"));
        }
    }

    private void checkObject(String key, String subject) {
        try {
            JsonObject object = client.getObject(key);
            if (object == null) {
                errors.append("-> ").append(subject).append(" ").append(key).append(" doesn't exist\r\n");
            }
        } catch (OrtolangClientException | OrtolangClientAccountException e) {
            errors.append("-> unable to find ").append(key).append(" : ").append(e.getMessage()).append("\r\n");
        }
    }

    public static List<String> extractOrtolangKeys(String json) {
        Matcher okMatcher = CheckBagCommand.ORTOLANG_KEY_MATCHER.matcher(json);
        List<String> ortolangKeys = new ArrayList<>();
        while (okMatcher.find()) {
            if (!ortolangKeys.contains(okMatcher.group(1))) {
                ortolangKeys.add(okMatcher.group(1));
            }
        }
        return ortolangKeys;
    }

    public static final String getContent(Path filepath) {
        String content = null;
        try (InputStream is = Files.newInputStream(filepath)) {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            System.out.println("  unable to get content of file : " + filepath + " : " + e.getMessage());
        }
        return content;
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Check Bag", options);
        System.exit(0);
    }

    public static class Template {

        static Map<String, Template> templates = new HashMap<>();

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
                if (template.getLevel() == level) {
                    return template;
                }
            }
            return templates.get("forall");
        }

    }

}
