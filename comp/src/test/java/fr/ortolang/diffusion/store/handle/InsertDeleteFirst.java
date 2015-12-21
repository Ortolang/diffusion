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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

public class InsertDeleteFirst {
    
    public static void main(String args[]) {
        Path input = Paths.get("/home/jerome/ortolang_sldr_handles_new.sql");
        Path output = Paths.get("/home/jerome/ortolang_sldr_handles_new_with_delete.sql");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int nbreads = 0;
        try (InputStream fis = Files.newInputStream(input)) {
            while ( (nbreads=fis.read(buf)) != -1 ) {
                baos.write(buf, 0, nbreads);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String content = baos.toString();
        StringTokenizer lines = new StringTokenizer(content, "\r\n");
        
        try ( OutputStream os = Files.newOutputStream(output)) {
            while ( lines.hasMoreTokens() ) {
                String line = lines.nextToken();
                if ( line.startsWith("INSERT") ) {
                    String[] parts = line.split(",");
                    String handle = parts[0].substring(parts[0].indexOf("(")+1);
                    String idx = parts[1].trim();
                    String sql = "DELETE FROM handles WHERE handle=" + handle + " AND idx = " + idx + ";\r\n";
                    os.write(sql.getBytes());
                } 
                os.write(line.getBytes());
                os.write("\r\n".getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
