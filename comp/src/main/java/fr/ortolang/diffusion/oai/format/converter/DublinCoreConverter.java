package fr.ortolang.diffusion.oai.format.converter;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.DCXMLDocument;
import fr.ortolang.diffusion.oai.format.OLAC;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.oai.format.handler.DublinCoreHandler;

public class DublinCoreConverter implements MetadataConverter {

	@Override
	public void convert(String source, String format, MetadataBuilder builder) throws MetadataConverterException {
		if ( MetadataFormat.OLAC.equals(format)) {
			convertToOlac(source, builder);
		}
	}

	@Override
	public boolean isCompatible(String format) {
		return MetadataFormat.OLAC.equals(format);
	}
	
	private void convertToOlac(String source, MetadataBuilder builder) throws MetadataConverterException {
		StringReader reader = new StringReader(source);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			DublinCoreHandler.writeDcDocument(builder);
			JsonObject jsonDoc = jsonReader.readObject();

			// Converts elements from OLAC to DC based on OLAC-to-OAI_DC
			// crosswalk
			// [http://www.language-archives.org/NOTE/olac_display.html]
			if (jsonDoc.containsKey("type")) {
				JsonArray elmArray = jsonDoc.getJsonArray("type");
				for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
					if (elm.containsKey("type")) {
						String xsitype = elm.getString("type");
						if ("olac:linguistic-type".equals(xsitype) && elm.containsKey("code")) {
							// Rules 1 & 2
							String value = "Linguistic type:" + elm.getString("code").replaceAll("_", " ");
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "type", value);
						} else if ("olac:discourse-type".equals(xsitype)) {
							// Rules 1 & 3
							String value = "Discourse type:" + elm.getString("code").replaceAll("_", " ");
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "description", value);
						}
					}
				}
			}
			if (jsonDoc.containsKey("subject")) {
				JsonArray elmArray = jsonDoc.getJsonArray("subject");
				for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
					if (elm.containsKey("type")) {
						String xsitype = elm.getString("type");
						if ("olac:discourse-type".equals(xsitype) && elm.containsKey("code")) {
							// Rules 1 & 2
							String value = "Discourse type:" + elm.getString("code").replaceAll("_", " ");
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "subject", value);
						} else if ("olac:linguistic-field".equals(xsitype)) {
							// Rules 1
							String value = elm.getString("code").replaceAll("_", " ");
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "subject", value);
						}
					}
				}
			}
			// Rules 4
			if (jsonDoc.containsKey("contributor")) {
				JsonArray elmArray = jsonDoc.getJsonArray("contributor");
				for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
					if (elm.containsKey("code") && "author".equals(elm.getString("code")) && elm.containsKey("value")) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "creator", elm.getString("value"));
					} else if (elm.containsKey("value")) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "contributor", elm.getString("value"));
					}
				}
			}
			// Rules 5
			if (jsonDoc.containsKey("subject")) {
				JsonArray elmArray = jsonDoc.getJsonArray("subject");
				for (JsonObject elmSubject : elmArray.getValuesAs(JsonObject.class)) {
					if (elmSubject.containsKey("type") && elmSubject.getString("type").equals("olac:language")) {
						if (jsonDoc.containsKey("language")) {
							JsonArray elmLanguageArray = jsonDoc.getJsonArray("language");
							for (JsonObject elmLanguage : elmLanguageArray.getValuesAs(JsonObject.class)) {
								if (elmLanguage.containsKey("type")
										&& elmLanguage.getString("type").equals("olac:language")) {
									if (elmSubject.containsKey("code") && elmLanguage.containsKey("code")
											&& elmSubject.getString("code").equals(elmLanguage.getString("code"))) {
										break;
									}
								}
							}
						}
					}
				}
			}
			if (jsonDoc.containsKey("language")) {
				JsonArray elmArray = jsonDoc.getJsonArray("language");
				for (JsonObject elmLanguage : elmArray.getValuesAs(JsonObject.class)) {
					if (elmLanguage.containsKey("type") && elmLanguage.getString("type").equals("olac:language")
							&& elmLanguage.containsKey("code")) {
						builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "language", elmLanguage.getString("code"));
					}
				}
			}

			// Rules 6
			for (String dateElementName : OLAC.DATE_ELEMENTS) {
				if (jsonDoc.containsKey(dateElementName)) {
					JsonArray elmArray = jsonDoc.getJsonArray(dateElementName);
					for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
						if (elm.containsKey("value")) {
							builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "date", elm.getString("value"));
						}
					}
					break;
				}
			}
			// otherwise add DC elements
			for (String elm : DCXMLDocument.DC_ELEMENTS) {
				if (!"contributor".equals(elm) && !"date".equals(elm)) {
					DublinCoreHandler.writeDcElement(elm, jsonDoc, builder);
				}
			}
			// and convert DCTERMS to DC
			for (Map.Entry<List<String>, String> elm : OLAC.OLAC_TO_DC_ELEMENTS.entrySet()) {
				for (String olacElement : elm.getKey()) {
					DublinCoreHandler.writeDcElement(olacElement, jsonDoc, elm.getValue(), builder);
				}
			}
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataConverterException("unable to build OAI_DC from json", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

}
