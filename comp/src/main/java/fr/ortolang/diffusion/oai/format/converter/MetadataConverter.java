package fr.ortolang.diffusion.oai.format.converter;

import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;

public interface MetadataConverter {

	void convert(String source, String format, MetadataBuilder builder) throws MetadataConverterException;
	boolean isCompatible(String format);
}
