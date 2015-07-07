package fr.ortolang.diffusion.core.preview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import fr.ortolang.diffusion.thumbnail.generator.ThumbnailGeneratorException;
import fr.ortolang.diffusion.thumbnail.generator.VideoThumbnailGenerator;

public class VideoPreviewGeneratorTest {

	private static final Logger LOGGER = Logger.getLogger(VideoPreviewGeneratorTest.class.getName());
	
	@Test 
	public void testGenerateMP4() throws IOException, ThumbnailGeneratorException {
		VideoThumbnailGenerator generator = new VideoThumbnailGenerator();
		Path out = Files.createTempFile("preview-mp4-", ".jpg");
		Path in = Paths.get(this.getClass().getClassLoader().getResource("small.mp4").getFile());
		generator.generate(in.toFile(), out.toFile(), 300, 300);
		LOGGER.log(Level.INFO, "preview generated");
	}
	
	@Test 
	public void testGenerateOGV() throws IOException, ThumbnailGeneratorException {
		VideoThumbnailGenerator generator = new VideoThumbnailGenerator();
		Path out = Files.createTempFile("preview-ogv-", ".jpg");
		Path in = Paths.get(this.getClass().getClassLoader().getResource("small.ogv").getFile());
		generator.generate(in.toFile(), out.toFile(), 300, 300);
		LOGGER.log(Level.INFO, "preview generated");
	}

}
