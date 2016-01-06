package fr.ortolang.diffusion.store.handle;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;

public class GenerateHandleMetadata {

    public static void main(String args[]) {
        Path input = Paths.get("/media/space/jerome/Data/Comere/src/properties/handles.new.txt");
        Path objects = Paths.get("/media/space/jerome/Data/Comere/src/objects");
        Path metadata = Paths.get("/media/space/jerome/Data/Comere/src/metadata");

        Map<String, String> handles = readHandles(input);
        try {
            Files.walkFileTree(objects, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativeFile = objects.relativize(file).toString();
                    if (handles.containsKey(relativeFile)) {
                        // System.out.println("Found a handle for file ath path : " + file);
                        generateHandleMetadata(Paths.get(metadata.toString(), relativeFile), handles.get(relativeFile));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public static void generateHandleMetadata(Path path, String handle) {
        if (!handle.contains(".") || !path.toString().endsWith(handle.substring(12))) {
            //System.out.println("Need to create specific handle metadata : " + path + " => " + handle);
            try {
                path.toFile().mkdirs();
                Path metadata = Paths.get(path.toString(), "ortolang-pid-json");
                JsonWriter writer = Json.createWriter(Files.newOutputStream(metadata));
                JsonObject pid = Json.createObjectBuilder().add("pids", Json.createArrayBuilder().add(Json.createObjectBuilder().add("type", "handle").add("value", handle).build()).build()).build();
                writer.writeObject(pid);
                writer.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } else {
            System.out.println("Handle will be auto generated : " + path + " => " + handle);
        }
    }

    public static Map<String, String> readHandles(Path input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int nbreads = 0;
        try (InputStream fis = Files.newInputStream(input)) {
            while ((nbreads = fis.read(buf)) != -1) {
                baos.write(buf, 0, nbreads);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> handles = new HashMap<String, String>();
        StringTokenizer lines = new StringTokenizer(baos.toString(), "\r\n");
        while (lines.hasMoreTokens()) {
            String line = lines.nextToken();
            String[] words = line.split("\t");
            handles.put(words[0], words[1].substring(22));
        }
        return handles;
    }

}
