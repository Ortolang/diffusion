package fr.ortolang.diffusion.preview.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import fr.ortolang.diffusion.preview.util.ImageResizer;

public class VideoPreviewGenerator implements PreviewGenerator {

	public void generate(File input, File output, int width, int height) throws PreviewGeneratorException {
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(input);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			g.start();
			Frame f = g.grabKeyFrame();
			Java2DFrameConverter converter = new Java2DFrameConverter();
			ImageIO.write(converter.getBufferedImage(f), "jpg", baos);
			g.stop();
			
			ImageResizer resizer = new ImageResizer(width, height);
			resizer.setInputImage(new ByteArrayInputStream(baos.toByteArray()));
			resizer.writeOutput(output);
		} catch (Exception e) {
			throw new PreviewGeneratorException("unable to generate video preview", e);
		}
	}

	public List<String> getAcceptedMIMETypes() {
    	return Arrays.asList(new String[] {
			      "video/mp4",
			      "video/quicktime",
			      "video/x-msvideo",
			      "video/x-ms-wmv",
			      "video/mpeg",
			      "video/x-matroska",
			      "video/ogg"
			      });
	}
}
