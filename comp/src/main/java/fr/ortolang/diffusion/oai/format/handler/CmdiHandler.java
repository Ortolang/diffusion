package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class CmdiHandler implements MetadataHandler {

	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {

		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			writeCmdiDocument(builder);
			writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, builder);
			//TODO writeCmdiResources : for each identifier ?
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_JOURNALFILEPROXYLIST_ELEMENT);
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCERELATIONLISTT_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);
			
			writeCmdiOlacElement("title", jsonDoc, builder);
			
			builder.writeEndElement(); // OLAC-DcmiTerms
			builder.writeEndElement(); // Components
			
        	builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build CMID metadata format", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
		// TODO Auto-generated method stub
		
	}

	public static void writeCmdiDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(Constant.CMDI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.CMDI_NAMESPACE_URI, Constant.CMDI_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.CMDI_OLAC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.CMDI_OLAC_NAMESPACE_URI, Constant.CMDI_OLAC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		attrs.put(Constant.CMDI_VERSION_ATTRIBUTE, "1.2");
		builder.writeStartDocument(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_ELEMENT, attrs);
	}
	
	public static void writeCmdiHeader(String mdCreator, MetadataBuilder builder) throws MetadataBuilderException {
		builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_HEADER_ELEMENT);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCREATOR_ELEMENT, null, mdCreator);
		//TODO format date
//		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCREATIONDATE_ELEMENT, null, System.currentTimeMillis());
		//TODO get key to MdSelfLink
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDPROFILE_ELEMENT, Constant.CMDI_OLAC_PROFILE_VALUE);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCOLLECTIONDISPLAYNAME_ELEMENT, null, mdCreator);
		builder.writeEndElement(); // End of Header
	}

	public static void writeCmdiOlacElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeCmdiOlacElement(elementName, meta, elementName, builder);
	}
	
	public static void writeCmdiOlacElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
				if (elm.containsKey("lang") && elm.containsKey("value")) {
					writeCmdiOlacMultilingualElement(tagName, elm, builder);
				} else {
					if (elm.containsKey("value")) {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
					}
				}
			}
		}
	}

	public static void writeCmdiOlacMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("xml:lang", multilingualObject.getString("lang"));
		builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}
}
