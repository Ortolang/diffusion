package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;
import java.util.Date;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

/**
 * Writes DublinCore metadata.
 * @author cpestel
 */
public class DublinCoreHandler implements MetadataHandler {

	public DublinCoreHandler() { }

	/**
	 * Converts a JSON (string representation) metadata object to an XML DublinCore metadata object.
	 * @param item
	 * @return
	 */
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {
//		OAI_DC oai_dc = new OAI_DC();
		
		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonDoc = jsonReader.readObject();

		try {
			writeDcDocument(builder);
			
//			JsonArray multilingualTitles = jsonDoc.getJsonArray("title");
//			for (JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {

//				oai_dc.addDcMultilingualField("title", multilingualTitle.getString("lang"),
//						XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
				writeDcElement("title", jsonDoc, builder);
//			}
//
//			JsonArray multilingualDescriptions = jsonDoc.getJsonArray("description");
//			for (JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//				oai_dc.addDcMultilingualField("description", multilingualTitle.getString("lang"),
//						XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
//			}
//
//			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
//			if (corporaLanguages != null) {
//				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
//					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");
//
//					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//						oai_dc.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//						oai_dc.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
//					}
//					oai_dc.addDcField("language", corporaLanguage.getString("id"));
//				}
//			}
//
//			JsonArray multilingualKeywords = jsonDoc.getJsonArray("keywords");
//			if (multilingualKeywords != null) {
//				for (JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
//					oai_dc.addDcMultilingualField("subject", multilingualKeyword.getString("lang"),
//							multilingualKeyword.getString("value"));
//				}
//			}
//
//			JsonArray producers = jsonDoc.getJsonArray("producers");
//			if (producers != null) {
//				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
//					// JsonObject metaOrganization =
//					// producer.getJsonObject("meta_ortolang-referential-json");
//
//					if (producer.containsKey("fullname")) {
//						oai_dc.addDcField("publisher", producer.getString("fullname"));
//					}
//				}
//			}
//
//			JsonArray contributors = jsonDoc.getJsonArray("contributors");
//			if (contributors != null) {
//				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
//					JsonArray roles = contributor.getJsonArray("roles");
//					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
//						// JsonObject metaRole =
//						// role.getJsonObject("meta_ortolang-referential-json");
//						String roleId = role.getString("id");
//						oai_dc.addDcField("contributor", OAI_DC.person(contributor) + " (" + roleId + ")");
//
//						if ("author".equals(roleId)) {
//							oai_dc.addDcField("creator", OAI_DC.person(contributor));
//						}
//					}
//				}
//			}
//
//			JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
//			if (statusOfUse != null) {
//				String idStatusOfUse = statusOfUse.getString("id");
//				oai_dc.addDcField("rights", idStatusOfUse);
//
//				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
//				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//					oai_dc.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
//				}
//			}
//			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
//			if (conditionsOfUse != null) {
//				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
//					oai_dc.addDcMultilingualField("rights", label.getString("lang"),
//							XMLDocument.removeHTMLTag(label.getString("value")));
//				}
//			}
//
//			JsonObject license = jsonDoc.getJsonObject("license");
//			if (license != null) {
//				if (license != null) {
//					oai_dc.addDcMultilingualField("rights", "fr", license.getString("label"));
//				}
//			}
//
//			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
//			if (linguisticSubjects != null) {
//				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
//					oai_dc.addDcField("subject", "linguistic field: " + linguisticSubject.getString());
//				}
//			}
//			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
//			if (linguisticDataType != null) {
//				oai_dc.addDcField("type", "linguistic-type: " + linguisticDataType.getString());
//			}
//			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
//			if (discourseTypes != null) {
//				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
//					oai_dc.addDcField("type", "discourse-type: " + discourseType.getString());
//				}
//			}
//			JsonString creationDate = jsonDoc.getJsonString("originDate");
//			if (creationDate != null) {
//				oai_dc.addDcField("date", creationDate.getString());
//			} else {
//				JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
//				if (publicationDate != null) {
//					oai_dc.addDcField("date", publicationDate.getString());
//				}
//			}

			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build OAI_DC", e);
		} finally {
			jsonReader.close();
			reader.close();
		}

//		return oai_dc;
	}

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
		StringReader reader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			writeDcDocument(builder);
			JsonObject jsonDoc = jsonReader.readObject();
			for (String elm : Constant.DC_ELEMENTS) {
				writeDcElement(elm, jsonDoc, builder);
			}
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to write DublinCore metadata", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}
	
	public static void writeDcDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(Constant.OAI_DC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.OAI_DC_NAMESPACE_URI, Constant.OAI_DC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.DC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.DC_NAMESPACE_URI, Constant.DC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		builder.writeStartDocument(Constant.OAI_DC_NAMESPACE_PREFIX, Constant.OAI_DC_ELEMENT, null);
	}

	public static void writeDcElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeDcElement(elementName, meta, elementName, builder);
	}
	
	public static void writeDcElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
				if (elm.containsKey("lang") && elm.containsKey("value")) {
					writeDcMultilingualElement(tagName, elm, builder);
				} else {
					if (elm.containsKey("value")) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
					}
				}
			}
		}
	}
	
	public static void writeDcMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("xml:lang", multilingualObject.getString("lang"));
		builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}

}
