package fr.ortolang.diffusion.oai.format.converter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
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

			// Merge DC and DCTERMS, then sort by
			List<String> allElements = new ArrayList<String>(Constant.DCTERMS_ELEMENTS);
			allElements.addAll(Constant.DC_ELEMENTS);
			Collections.sort(allElements);
			
			for(String elm : allElements) {
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
