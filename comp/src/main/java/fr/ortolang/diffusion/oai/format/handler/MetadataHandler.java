package fr.ortolang.diffusion.oai.format.handler;

import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;

public interface MetadataHandler {

	void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException;
	void write(String json, MetadataBuilder builder) throws MetadataHandlerException;
}
