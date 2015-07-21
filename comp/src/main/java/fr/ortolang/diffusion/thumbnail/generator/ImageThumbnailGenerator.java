package fr.ortolang.diffusion.thumbnail.generator;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import fr.ortolang.diffusion.thumbnail.util.ImageResizer;

public class ImageThumbnailGenerator implements ThumbnailGenerator {

	public void generate(File input, File output, int width, int height) throws ThumbnailGeneratorException {
		ImageResizer resizer = new ImageResizer(width, height);
		try {
			resizer.setInputImage(input);
			resizer.writeOutput(output);
		} catch (Exception e) {
			throw new ThumbnailGeneratorException("unable to generate an image preview", e);
		}
	}

    public List<String> getAcceptedMIMETypes() {
		return Arrays.asList(ImageIO.getReaderMIMETypes());
	}
}