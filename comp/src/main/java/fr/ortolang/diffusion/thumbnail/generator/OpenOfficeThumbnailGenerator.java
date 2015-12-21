package fr.ortolang.diffusion.thumbnail.generator;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import fr.ortolang.diffusion.thumbnail.util.ImageResizer;

public class OpenOfficeThumbnailGenerator implements ThumbnailGenerator {

	public void generate(File input, File output, int width, int height) throws ThumbnailGeneratorException {
		try (ZipFile zipFile = new ZipFile(input)) {
			ZipEntry entry = zipFile.getEntry("Thumbnails/thumbnail.png");
			if (entry == null) {
				throw new ThumbnailGeneratorException("Zip file does not contain 'Thumbnails/thumbnail.png' . Is this really an OpenOffice-File?");
			}
			
			try (InputStream is = new BufferedInputStream(zipFile.getInputStream(entry)) ) {
				ImageResizer resizer = new ImageResizer(width, height);
				resizer.setInputImage(is);
				resizer.writeOutput(output);
			} catch (Exception e) {
				throw new ThumbnailGeneratorException("Error during reading zip entry", e);
			}
		} catch ( ZipException e ) {
			throw new ThumbnailGeneratorException("This is not a zipped file. Is this really an OpenOffice-File?", e);
		} catch ( IOException e ) {
			throw new ThumbnailGeneratorException("Error during reading zip file", e);
		}
	}

	public List<String> getAcceptedMIMETypes() {
    	return Arrays.asList(new String[] {
			      "application/vnd.sun.xml.writer",
			      "application/vnd.sun.xml.writer.template",
			      "application/vnd.sun.xml.writer.global",
			      "application/vnd.sun.xml.calc",
			      "application/vnd.sun.xml.calc.template",
			      "application/vnd.stardivision.calc",
			      "application/vnd.sun.xml.impress",
			      "application/vnd.sun.xml.impress.template ",
			      "application/vnd.stardivision.impress sdd",
			      "application/vnd.sun.xml.draw",
			      "application/vnd.sun.xml.draw.template",
			      "application/vnd.stardivision.draw",
			      "application/vnd.sun.xml.math",
			      "application/vnd.stardivision.math",
			      "application/vnd.oasis.opendocument.text",
			      "application/vnd.oasis.opendocument.text-template",
			      "application/vnd.oasis.opendocument.text-web",
			      "application/vnd.oasis.opendocument.text-master",
			      "application/vnd.oasis.opendocument.graphics",
			      "application/vnd.oasis.opendocument.graphics-template",
			      "application/vnd.oasis.opendocument.presentation",
			      "application/vnd.oasis.opendocument.presentation-template",
			      "application/vnd.oasis.opendocument.spreadsheet",
			      "application/vnd.oasis.opendocument.spreadsheet-template",
			      "application/vnd.oasis.opendocument.chart",
			      "application/vnd.oasis.opendocument.formula",
			      "application/vnd.oasis.opendocument.database",
			      "application/vnd.oasis.opendocument.image",
			      "application/zip"
			      });
	}
}
