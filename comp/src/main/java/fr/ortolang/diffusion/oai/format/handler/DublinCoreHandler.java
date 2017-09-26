package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.util.DateUtils;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

/**
 * Writes DublinCore metadata.
 * @author cpestel
 */
public class DublinCoreHandler implements MetadataHandler {

    private static final Logger LOGGER = Logger.getLogger(DublinCoreHandler.class.getName());

	private List<String> listHandles;
	
	public DublinCoreHandler() { }

	/**
	 * Converts a JSON (string representation) metadata object to an XML DublinCore metadata object.
	 * @param item
	 * @return
	 */
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {
		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonDoc = jsonReader.readObject();

		try {
			writeDcDocument(builder);
			
			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}
			
			writeDcElement("title", jsonDoc, builder);
			writeDcElement("description", jsonDoc, builder);
			writeDcElement("keywords", jsonDoc, "subject", builder);

			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						writeDcMultilingualElement("subject", label, builder);
						writeDcMultilingualElement("language", label, builder);
					}
					builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "language", corporaLanguage.getString("id"));
				}
			}

			JsonArray producers = jsonDoc.getJsonArray("producers");
			if (producers != null) {
				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
					if (producer.containsKey("fullname")) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
					}
				}
			}

			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			if (contributors != null) {
				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
					JsonArray roles = contributor.getJsonArray("roles");
					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
						String roleId = role.getString("id");
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "contributor", Constant.person(contributor) + " (" + roleId + ")");

						if ("author".equals(roleId)) {
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "creator", Constant.person(contributor));
						}
					}
				}
			}

			JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
					writeDcMultilingualElement("rights", label, builder);
				}
			}
			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
			if (conditionsOfUse != null) {
				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
					writeDcMultilingualElement("rights", label, builder);
				}
			}
			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null) {
				if (license != null) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", "fr");
					builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "rights", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
				}
			}
			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "subject", "linguistic field: " + linguisticSubject.getString());
				}
			}
			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "type", "linguistic-type: " + linguisticDataType.getString());
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "type", "discourse-type: " + discourseType.getString());
				}
			}
			JsonString creationDate = jsonDoc.getJsonString("originDate");
			if (creationDate != null) {
				builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "date", creationDate.getString());
			} else {
				JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
				if (publicationDate != null) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "date", publicationDate.getString());
				}
			}
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build OAI_DC", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
		StringReader reader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			writeDcDocument(builder);
			JsonObject jsonDoc = jsonReader.readObject();

			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}
			
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

	public List<String> getListHandlesRoot() {
		return listHandles;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandles = listHandlesRoot;
	}

}
