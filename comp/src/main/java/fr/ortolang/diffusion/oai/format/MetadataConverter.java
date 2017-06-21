package fr.ortolang.diffusion.oai.format;

import fr.ortolang.diffusion.oai.exception.MetadataConverterException;

public interface MetadataConverter {

	void convert(String source, String format, MetadataBuilder builder) throws MetadataConverterException;
	boolean isCompatible(String format);
}
