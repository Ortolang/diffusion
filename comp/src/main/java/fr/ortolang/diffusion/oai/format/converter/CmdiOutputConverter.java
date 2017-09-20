package fr.ortolang.diffusion.oai.format.converter;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.oai.format.handler.CmdiHandler;

/**
 * Converts to OLAC metadata.
 * @author cpestel
 *
 */
public class CmdiOutputConverter implements MetadataConverter {

    private static final Logger LOGGER = Logger.getLogger(CmdiOutputConverter.class.getName());

    private String id;
	private List<String> listHandles;
	
	@Override
	public void convert(String source, String format, MetadataBuilder builder) throws MetadataConverterException {
		if (MetadataFormat.OLAC.equals(format)) {
			convertFromOlac(source, builder);
		} else if (MetadataFormat.OAI_DC.equals(format)) {
			convertFromDc(source, builder);
		}
	}

	@Override
	public boolean isCompatible(String format) {
		return MetadataFormat.OLAC.equals(format) || MetadataFormat.OAI_DC.equals(format);
	}

	private void convertFromDc(String source, MetadataBuilder builder) throws MetadataConverterException {
		StringReader reader = new StringReader(source);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			CmdiHandler.writeCmdiDocument(builder);
			CmdiHandler.writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, id, Constant.w3cdtf.format(new Date()), builder);
			CmdiHandler.writeCmdiResources(listHandles, builder);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);

			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}
			
			for (String elm : Constant.DC_ELEMENTS) {
				CmdiHandler.writeCmdiOlacElement(elm, jsonDoc, builder);
			}
			
			builder.writeEndElement(); // OLAC-DcmiTerms
			builder.writeEndElement(); // Components
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataConverterException("unable to build CMDI from OLAC", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}
	
	private void convertFromOlac(String source, MetadataBuilder builder) throws MetadataConverterException {
		StringReader reader = new StringReader(source);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			CmdiHandler.writeCmdiDocument(builder);
			CmdiHandler.writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, id, Constant.w3cdtf.format(new Date()), builder);
			CmdiHandler.writeCmdiResources(listHandles, builder);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);

			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}
			//TODO write olac-linguistic-*, dcterms*
			
        	// DCTerms elements
        	for(String dcterms : Constant.DCTERMS_ELEMENTS) {
        		CmdiHandler.writeCmdiOlacElement(dcterms, jsonDoc, builder);
        	}
        	// Dublin Core elements with OLAC attributes
        	for(String dc : Constant.DC_ELEMENTS) {
        		CmdiHandler.writeCmdiOlacElement(dc, jsonDoc, builder);
        	}
			
			builder.writeEndElement(); // OLAC-DcmiTerms
			builder.writeEndElement(); // Components
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataConverterException("unable to build CMDI from OLAC", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	public List<String> getListHandles() {
		return listHandles;
	}

	public void setListHandles(List<String> listHandles) {
		this.listHandles = listHandles;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
