package fr.ortolang.diffusion.thumbnail.generator;

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
