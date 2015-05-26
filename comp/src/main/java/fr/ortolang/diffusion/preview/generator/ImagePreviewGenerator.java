package fr.ortolang.diffusion.preview.generator;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import fr.ortolang.diffusion.preview.util.ImageResizer;

public class ImagePreviewGenerator implements PreviewGenerator {

	public void generate(File input, File output, int width, int height) throws PreviewGeneratorException {
		ImageResizer resizer = new ImageResizer(width, height);
		try {
			resizer.setInputImage(input);
			resizer.writeOutput(output);
		} catch (Exception e) {
			throw new PreviewGeneratorException("unable to generate an image preview", e);
		}
	}

    public List<String> getAcceptedMIMETypes() {
		return Arrays.asList(ImageIO.getReaderMIMETypes());
	}
}