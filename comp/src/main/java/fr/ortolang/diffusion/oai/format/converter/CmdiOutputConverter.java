package fr.ortolang.diffusion.oai.format.converter;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.oai.format.handler.CmdiHandler;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

/**
 * Converts to OLAC metadata.
 * @author cpestel
 *
 */
public class CmdiOutputConverter implements MetadataConverter {

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
			CmdiHandler.writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, builder);
			//TODO writeCmdiResources : for each identifier ?
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_JOURNALFILEPROXYLIST_ELEMENT);
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCERELATIONLISTT_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);
			
			CmdiHandler.writeCmdiOlacElement("title", jsonDoc, builder);
			
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
			CmdiHandler.writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, builder);
			//TODO writeCmdiResources : for each identifier ?
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_JOURNALFILEPROXYLIST_ELEMENT);
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCERELATIONLISTT_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);
			
			CmdiHandler.writeCmdiOlacElement("title", jsonDoc, builder);
			
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
}
