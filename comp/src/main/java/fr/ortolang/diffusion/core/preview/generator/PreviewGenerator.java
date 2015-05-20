package fr.ortolang.diffusion.core.preview.generator;

import java.io.File;
import java.util.List;

public interface PreviewGenerator {
	
	public void generate(File input, File output, int width, int height) throws PreviewGeneratorException;

	public List<String> getAcceptedMIMETypes();

}
