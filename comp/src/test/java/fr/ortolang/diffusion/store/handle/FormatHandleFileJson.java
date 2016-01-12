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

public class FormatHandleFileJson {
	
	public static void main(String args[]) {
		Path input = Paths.get("/home/jerome/handles.new.txt");
		Path output = Paths.get("/home/jerome/handles.new.json");
		Path log = Paths.get("/home/jerome/handles.new.log");
		
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
		
		try ( OutputStream os = Files.newOutputStream(output); OutputStream logos = Files.newOutputStream(log) ) {
		    StringBuffer record = new StringBuffer();
		    record.append("[\r\n");
		    boolean last = false;
			while ( !last ) {
			    String line = lines.nextToken();
				String[] words = line.split("\t");
				
				record.append("{\"handle\":\"").append(words[1].substring(22)).append("\",\"url\":\"https://repository.ortolang.fr/api/content/comere/latest/").append(words[0]).append("\"},\r\n");
				if ( words[1].length() >= 35 ) {
				    record.append("{\"handle\":\"11403/comere/v1/").append(words[1].substring(35)).append("\",\"url\":\"https://repository.ortolang.fr/api/content/comere/v1/").append(words[0]).append("\"");
				} else {
				    record.append("{\"handle\":\"11403/comere/v1").append("\",\"url\":\"https://repository.ortolang.fr/api/content/comere/v1/").append(words[0]).append("\"");
				}
				last = !lines.hasMoreTokens();
                if ( !last ) {
				    record.append("},\r\n");
				} else {
				    record.append("}\r\n");
				}
				
				logos.write(line.getBytes());
				logos.write("\n".getBytes());
			}
			record.append("]");
			os.write(record.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
