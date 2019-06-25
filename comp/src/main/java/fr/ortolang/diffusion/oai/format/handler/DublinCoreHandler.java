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
import static fr.ortolang.diffusion.oai.format.Constant.*;
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
					if (corporaLanguage.containsKey("id")) {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "language", corporaLanguage.getString("id"));
					} else {
						LOGGER.log(Level.SEVERE, "corporaLanguage missing id for " + corporaLanguage.toString());
					}
				}
			}
			
			JsonArray lexiconInputLanguages = jsonDoc.getJsonArray("lexiconInputLanguages");
			if (lexiconInputLanguages != null) {
				for (JsonObject lexiconInputLanguage : lexiconInputLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = lexiconInputLanguage.getJsonArray("labels");
					
					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						writeDcMultilingualElement("subject", label, builder);
						writeDcMultilingualElement("language", label, builder);
					}
					if (lexiconInputLanguage.containsKey("id")) {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "language", lexiconInputLanguage.getString("id"));
					} else {
						LOGGER.log(Level.SEVERE, "lexiconInputLanguage missing id for " + lexiconInputLanguage.toString());
					}
				}
			}

			JsonArray producers = jsonDoc.getJsonArray("producers");
			if (producers != null) {
				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
					if (producer.containsKey("fullname")) {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
					}
				}
			}

			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			if (contributors != null) {
				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
					JsonArray roles = contributor.getJsonArray("roles");
					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
						String roleId = role.getString("id");
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "contributor", person(contributor) + " (" + roleId + ")");

						if ("author".equals(roleId)) {
							builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "creator", person(contributor));
						}
					}
				}
			}

			JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

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
			if (license != null && license.containsKey("label")) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("xml:lang", "fr");
				builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "rights", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
			}
			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "subject", "linguistic field: " + linguisticSubject.getString());
				}
			}
			JsonString resourceType = jsonDoc.getJsonString("type");
			if (resourceType != null) {
				switch(resourceType.getString()) {
					case ORTOLANG_RESOURCE_TYPE_CORPORA:
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_CORPUS); break;
					case ORTOLANG_RESOURCE_TYPE_LEXICON:
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), OLAC_LINGUISTIC_TYPES.get(1)); break;
					case ORTOLANG_RESOURCE_TYPE_TERMINOLOGY:
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_TERMINOLOGY); break;
					case ORTOLANG_RESOURCE_TYPE_TOOL:
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_TOOL_SERVICE); break;
					case CMDI_RESOURCE_CLASS_WEBSITE:
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_WEBSITE); break;
				}
			}
			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "type", "linguistic-type: " + linguisticDataType.getString());
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "type", "discourse-type: " + discourseType.getString());
				}
			}
			JsonString creationDate = jsonDoc.getJsonString("originDate");
			if (creationDate != null) {
				builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "date", creationDate.getString());
			} else {
				JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
				if (publicationDate != null) {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "date", publicationDate.getString());
				}
			}

			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}
			
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build OAI_DC cause " + e.getMessage(), e);
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

			for (String elm : DC_ELEMENTS) {
				writeDcElement(elm, jsonDoc, builder);
			}

			if (listHandles != null) {
				listHandles.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
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
		namespaces.put(OAI_DC_NAMESPACE_PREFIX, new XmlDumpNamespace(OAI_DC_NAMESPACE_URI, OAI_DC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(DC_NAMESPACE_PREFIX, new XmlDumpNamespace(DC_NAMESPACE_URI, DC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		builder.writeStartDocument(OAI_DC_NAMESPACE_PREFIX, OAI_DC_ELEMENT, null);
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
						builder.writeStartEndElement(DC_NAMESPACE_PREFIX, tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
					}
				}
			}
		}
	}
	
	public static void writeDcMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		if (multilingualObject.getString("lang").matches(iso639_2pattern)) {
			attrs.put("xml:lang", multilingualObject.getString("lang"));
		}
		builder.writeStartEndElement(DC_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}

	public List<String> getListHandlesRoot() {
		return listHandles;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandles = listHandlesRoot;
	}

}
