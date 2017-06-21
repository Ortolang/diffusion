package fr.ortolang.diffusion.oai.format;

import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;

public interface MetadataHandler {

	void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException;
	void write(String json, MetadataBuilder builder) throws MetadataHandlerException;
}
