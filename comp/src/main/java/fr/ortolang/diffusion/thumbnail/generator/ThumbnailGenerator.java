package fr.ortolang.diffusion.thumbnail.generator;

import java.io.File;
import java.util.List;

public interface ThumbnailGenerator {
	
	public void generate(File input, File output, int width, int height) throws ThumbnailGeneratorException;

	public List<String> getAcceptedMIMETypes();

}
