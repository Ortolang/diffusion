package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import static fr.ortolang.diffusion.oai.format.Constant.*;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.util.DateUtils;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class CmdiHandler implements MetadataHandler {

    private static final Logger LOGGER = Logger.getLogger(CmdiHandler.class.getName());

    private String id;
	private List<String> listHandlesRoot;
	
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {

		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			writeCmdiDocument(builder);
			writeCmdiHeader(CMDI_MDCREATOR_VALUE, id, w3cdtf.format(new Date()), builder);
			writeCmdiResources(listHandlesRoot, builder);
			
			builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(CMDI_OLAC_NAMESPACE_PREFIX, CMDI_OLAC_ELEMENT);

			JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
	        if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	            	XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", multilingualBibliographicCitation.getString("lang"));
					builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "bibliographicCitation", attrs, XMLDocument.removeHTMLTag(multilingualBibliographicCitation.getString("value")));
	            }
	        }
	        
			JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
	        if(sponsors!=null) {
	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
	            	if(sponsor.containsKey("fullname")) {
	            		XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-role", "sponsor");
				    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, sponsor.getString("fullname"));
	            	}
	            }
	        }

			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			if (contributors != null) {
				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
					JsonArray roles = contributor.getJsonArray("roles");
					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
						String roleId = role.getString("id");
						if (OLAC_ROLES.contains(roleId)) {
							XmlDumpAttributes attrs = new XmlDumpAttributes();
					    	attrs.put("olac-role", roleId);
					    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, person(contributor));
						} else {
							builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "contributor", person(contributor));
						}
					}
				}
			}
			
			JsonString creationDate = jsonDoc.getJsonString("originDate");
	        if(creationDate!=null) {
		        if (DateUtils.isThisDateValid(creationDate.getString())) {
		        	XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("dcterms-type", "W3CDTF");
		        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "date", attrs, creationDate.getString());
		        } else {
		        	LOGGER.log(Level.WARNING, "invalid creation date : " + creationDate.getString());
		        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "date", creationDate.getString());
		        }
	        } else {
	        	JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
	            if(publicationDate!=null) {
			        if (DateUtils.isThisDateValid(publicationDate.getString())) {
			        	XmlDumpAttributes attrs = new XmlDumpAttributes();
				        attrs.put("dcterms-type", "W3CDTF");
			        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "date", attrs, publicationDate.getString());
			        } else {
			        	LOGGER.log(Level.WARNING, "invalid publication date : " + publicationDate.getString());
			        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "date", publicationDate.getString());
			        }
	            }
	        }
	        
			writeCmdiOlacElement("description", jsonDoc, builder);
			
			JsonArray corporaFormats = jsonDoc.getJsonArray("corporaFormats");
			if (corporaFormats != null) {
				for (JsonObject corporaFormat : corporaFormats.getValuesAs(JsonObject.class)) {
					JsonArray mimetypes = corporaFormat.getJsonArray("mimetypes");
					if (mimetypes != null) {
						for (JsonString mimetype : mimetypes.getValuesAs(JsonString.class)) {
							builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "format", mimetype.getString());
						}
					}
				}
			}

			JsonArray lexiconFormats = jsonDoc.getJsonArray("lexiconFormats");
			if (lexiconFormats != null) {
				for (JsonObject lexiconFormat : lexiconFormats.getValuesAs(JsonObject.class)) {
					JsonArray mimetypes = lexiconFormat.getJsonArray("mimetypes");
					if (mimetypes != null) {
						for (JsonString mimetype : mimetypes.getValuesAs(JsonString.class)) {
							builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "format", mimetype.getString());
						}
					}
				}
			}
			
			if (listHandlesRoot != null) {
				listHandlesRoot.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}

			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						// Can't write subject here !!
//						writeCmdiOlacElement("subject", label, builder);
						
						XmlDumpAttributes attrs = new XmlDumpAttributes();
						if (corporaLanguage.containsKey("parentCode") && corporaLanguage.getString("parentCode").matches(iso639_3pattern)) {
					    	attrs.put("olac-language", corporaLanguage.getString("parentCode"));
						} else {
							attrs.put("olac-language", corporaLanguage.getString("id"));
						}
						
				    	if (label.containsKey("lang") && label.getString("lang").matches(iso639_2pattern)) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "language", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
					}
				}
			}

			JsonArray lexiconInputLanguages = jsonDoc.getJsonArray("lexiconInputLanguages");
			if (lexiconInputLanguages != null) {
				for (JsonObject lexiconInputLanguage : lexiconInputLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = lexiconInputLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						// Can't write subject here !! writeCmdiOlacElement("subject", label, builder);
						
						XmlDumpAttributes attrs = new XmlDumpAttributes();
						if (lexiconInputLanguage.containsKey("parentCode") && lexiconInputLanguage.getString("parentCode").matches(iso639_3pattern)) {
					    	attrs.put("olac-language", lexiconInputLanguage.getString("parentCode"));
						} else {
							attrs.put("olac-language", lexiconInputLanguage.getString("id"));
						}
				    	if (label.containsKey("lang") && label.getString("lang").matches(iso639_2pattern)) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "language", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
					}
				}
			}

			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null && license.containsKey("label")) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("xml:lang", "fr");
				builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "license", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
			}
			
			JsonArray producers = jsonDoc.getJsonArray("producers");
			if (producers != null) {
				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
					if (producer.containsKey("fullname")) {
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
					}
				}
			}

			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
			if (conditionsOfUse != null) {
				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}
			
	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}

			writeCmdiOlacElement("keywords", jsonDoc, "subject", builder);

			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					if (OLAC_LINGUISTIC_FIELDS.contains(linguisticSubject.getString())) {
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				        attrs.put("olac-linguistic-field", linguisticSubject.getString());
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, null);
					} else {
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "subject", linguisticSubject.getString());
					}
				}
			}
			
			JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
			if (studyLanguages != null) {
				for (JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = studyLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						XmlDumpAttributes attrs = new XmlDumpAttributes();
						if (studyLanguage.containsKey("parentCode") && studyLanguage.getString("parentCode").matches(iso639_3pattern)) {
					    	attrs.put("olac-language", studyLanguage.getString("parentCode"));
						} else {
							attrs.put("olac-language", studyLanguage.getString("id"));
						}
				    	if (label.containsKey("lang") && label.getString("lang").matches(iso639_2pattern)) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
					}
				}
			}
			
	        if(creationDate!=null) {
		        if (DateUtils.isThisDateValid(creationDate.getString())) {
		        	XmlDumpAttributes attrs = new XmlDumpAttributes();
		        	attrs.put("dcterms-type", "W3CDTF");
		        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "temporal", attrs, creationDate.getString());
		        } else {
		        	LOGGER.log(Level.WARNING, "invalid creation date (temporal) : " + creationDate.getString());
		        	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "temporal", creationDate.getString());
		        }
	        }
	        
			writeCmdiOlacElement("title", jsonDoc, builder);

			JsonString resourceType = jsonDoc.getJsonString("type");
			if (resourceType != null) {
				switch(resourceType.getString()) {
					case ORTOLANG_RESOURCE_TYPE_CORPORA:
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_CORPUS); break;
					case ORTOLANG_RESOURCE_TYPE_LEXICON:
						XmlDumpAttributes attrs = new XmlDumpAttributes();
						attrs.put("olac-linguistic-type", OLAC_LINGUISTIC_TYPES.get(1));
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrs, null);
						break;
					case ORTOLANG_RESOURCE_TYPE_TERMINOLOGY:
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_TERMINOLOGY); break;
					case ORTOLANG_RESOURCE_TYPE_TOOL:
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_TOOL_SERVICE); break;
					case CMDI_RESOURCE_CLASS_WEBSITE:
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), CMDI_RESOURCE_CLASS_WEBSITE); break;
				}
			}

			JsonArray corporaDataTypes = jsonDoc.getJsonArray("corporaDataTypes");
			if (corporaDataTypes != null) {
				for (JsonObject corporaDataType : corporaDataTypes.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaDataType.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {

						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	if (label.containsKey("lang") && label.getString("lang").matches(iso639_2pattern)) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, DC_ELEMENTS.get(14), attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
					}
				}
			}
			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				if (OLAC_LINGUISTIC_TYPES.contains(linguisticDataType.getString())) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
					attrs.put("olac-linguistic-type", linguisticDataType.getString());
					builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
				} else {
					builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "type", linguisticDataType.getString());
				}
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					if (OLAC_DISCOURSE_TYPES.contains(discourseType.getString())) {
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				        attrs.put("olac-discourse-type", discourseType.getString());
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
					} else {
						builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, "type", discourseType.getString());
					}
				}
			}
	        
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
		StringReader reader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			JsonObject CMD = jsonDoc.getJsonObject(CMDI_ELEMENT);
			
			String schemaLocation = CMDI_NAMESPACE_URI + " " + CMDI_NAMESPACE_SCHEMA_LOCATION;
			String cmdiVersion = CMDI_VERSION_VALUE_ATTRIBUTE;
			String cmdiXmlns = CMDI_NAMESPACE_URI;
			if (CMD != null && CMD.containsKey(XMLNS)) {
				cmdiXmlns = CMD.getString(XMLNS);
			}
			if (CMD != null && CMD.containsKey(XSI_SCHEMA_LOCATION)) {
				String[] schemaLocationSplit = CMD.getString(XSI_SCHEMA_LOCATION).split(" ");
				if (schemaLocationSplit.length > 1) {
					schemaLocation = schemaLocationSplit[1];
				}
			}
			if (CMD != null && CMD.containsKey(CMDI_VERSION_ATTRIBUTE)) {
				cmdiVersion = CMD.getString(CMDI_VERSION_ATTRIBUTE);
			}
			
			writeCmdiDocumentWithProfile(builder, cmdiXmlns, schemaLocation, cmdiVersion);
			if (CMD != null && CMD.containsKey(CMDI_HEADER_ELEMENT)) {
				writeCmdComponent(builder, CMDI_HEADER_ELEMENT, CMD.getJsonObject(CMDI_HEADER_ELEMENT));
			}
			JsonObject resourcesList = CMD.getJsonObject(CMDI_RESOURCES_ELEMENT);
			if (resourcesList != null) {
				JsonObject resourceProxyList = resourcesList.getJsonObject(CMDI_RESOURCEPROXYLIST_ELEMENT);
				if (resourceProxyList != null) {
					JsonArray resourceProxy = resourceProxyList.getJsonArray(CMDI_RESOURCEPROXY_ELEMENT);
					if (resourceProxy != null) {
						writeCmdResources(builder, resourceProxy);
					}
				}
			}
			
			if (CMD != null && CMD.containsKey(CMDI_COMPONENTS_ELEMENT)) {
				writeCmdComponent(builder, CMDI_COMPONENTS_ELEMENT, CMD.getJsonObject(CMDI_COMPONENTS_ELEMENT));
			}
			
        	builder.writeEndDocument();
			
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to write CmdiHandler metadata", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	public static void writeCmdComponent(MetadataBuilder builder, String name, JsonValue component) throws MetadataBuilderException {
		if (JsonValue.ValueType.OBJECT.equals(component.getValueType())) {
			XmlDumpAttributes attrs = new XmlDumpAttributes();
			if (((JsonObject) component).containsKey(CMDI_REF_ATTRIBUT)) {
				attrs.put(CMDI_REF_ATTRIBUT, ((JsonObject) component).getString(CMDI_REF_ATTRIBUT));
			}
			builder.writeStartElement(CMDI_NAMESPACE_PREFIX, name, attrs);
			for (Map.Entry<String, JsonValue> entry : ((JsonObject) component).entrySet()) {
				if (!entry.getKey().equals(CMDI_REF_ATTRIBUT)) {
					writeCmdComponent(builder, entry.getKey(), entry.getValue());
				}
			}
			builder.writeEndElement();
		} else if (JsonValue.ValueType.STRING.equals(component.getValueType())) {
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, name, null, ((JsonString) component).getString());
		} else if (JsonValue.ValueType.ARRAY.equals(component.getValueType())) {
			for ( JsonValue comp : (JsonArray) component) {
				writeCmdComponent(builder, name, comp);
			}
		}
	}
	
	public static void writeCmdResources(MetadataBuilder builder, JsonArray resources) throws MetadataBuilderException {
		builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCES_ELEMENT);
		
		if (resources != null && !resources.isEmpty()) {
			builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXYLIST_ELEMENT);
			
			for ( JsonValue comp : (JsonArray) resources) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
				attrs.put(CMDI_RESOURCEPROXYID_ELEMENT, ((JsonObject) comp).getString(CMDI_RESOURCEPROXYID_ELEMENT));
				builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXY_ELEMENT, attrs); // ResourceProxy
				
				JsonObject resourceType = ((JsonObject) comp).getJsonObject(CMDI_RESOURCETYPE_ELEMENT);
				if (resourceType != null) {
					XmlDumpAttributes attrsResType = new XmlDumpAttributes();
					String mimetype = resourceType.getString(CMDI_MIMETYPE_ATTRIBUT);
					String text = resourceType.getString(CMDI_TEXT_VALUE);
					if (mimetype != null) {
						attrsResType.put(CMDI_MIMETYPE_ATTRIBUT, mimetype);
					}
					builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCETYPE_ELEMENT, attrsResType, text);
					builder.writeEndElement(); // ResourceType
				}
				String resourceRef = ((JsonObject) comp).getString(CMDI_RESOURCEREF_ELEMENT);
				builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEREF_ELEMENT, resourceRef);
				
				builder.writeEndElement(); // ResourceProxy
			}
			builder.writeEndElement(); // ResourceProxyList
		}
		else {
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXYLIST_ELEMENT);
		}
		
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_JOURNALFILEPROXYLIST_ELEMENT);
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCERELATIONLISTT_ELEMENT);
		builder.writeEndElement();
	}
	
	public static void writeCmdiDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(CMDI_NAMESPACE_PREFIX, new XmlDumpNamespace(CMDI_NAMESPACE_URI, CMDI_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(CMDI_OLAC_NAMESPACE_PREFIX, new XmlDumpNamespace(CMDI_OLAC_NAMESPACE_URI, CMDI_OLAC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		attrs.put(CMDI_VERSION_ATTRIBUTE, CMDI_VERSION_VALUE_ATTRIBUTE);
		builder.writeStartDocument(CMDI_NAMESPACE_PREFIX, CMDI_ELEMENT, attrs);
	}

	public static void writeCmdiDocumentWithProfile(MetadataBuilder builder, String cmdiXmlns, String schemaLocation, String cmdiVersion) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(CMDI_NAMESPACE_PREFIX, new XmlDumpNamespace(cmdiXmlns, schemaLocation));
		namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		attrs.put(CMDI_VERSION_ATTRIBUTE, cmdiVersion);
		builder.writeStartDocument(CMDI_NAMESPACE_PREFIX, CMDI_ELEMENT, attrs);
	}
	
	public static void writeCmdiHeader(String mdCreator, String mdSelfLink, String mdCreationDate, MetadataBuilder builder) throws MetadataBuilderException {
		builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_HEADER_ELEMENT);
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_MDCREATOR_ELEMENT, null, mdCreator);
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_MDCREATIONDATE_ELEMENT, mdCreationDate);
		if (mdSelfLink != null) {
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_MDSELFLINK_ELEMENT, mdSelfLink);
		}
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_MDPROFILE_ELEMENT, CMDI_OLAC_PROFILE_VALUE);
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_MDCOLLECTIONDISPLAYNAME_ELEMENT, null, mdCreator);
		builder.writeEndElement(); // End of Header
	}

	public static void writeCmdiResources(List<String> ressources, MetadataBuilder builder) throws MetadataBuilderException {
		builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCES_ELEMENT);
		
		if (ressources != null && !ressources.isEmpty()) {
			builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXYLIST_ELEMENT);
			XmlDumpAttributes attrs = new XmlDumpAttributes();
			attrs.put(CMDI_RESOURCEPROXYID_ELEMENT, "_"+UUID.randomUUID().toString().substring(0,8));
			builder.writeStartElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXY_ELEMENT, attrs);
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCETYPE_ELEMENT, CMDI_RESOURCETYPE_LANDINGPAGE);
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEREF_ELEMENT, ressources.get(0));
			builder.writeEndElement();
			builder.writeEndElement();
		}
		else {
			builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCEPROXYLIST_ELEMENT);
		}
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_JOURNALFILEPROXYLIST_ELEMENT);
		builder.writeStartEndElement(CMDI_NAMESPACE_PREFIX, CMDI_RESOURCERELATIONLISTT_ELEMENT);
		builder.writeEndElement(); // End of Resources
	}
	
	public static void writeCmdiOlacElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeCmdiOlacElement(elementName, meta, elementName, builder);
	}
	
	public static void writeCmdiOlacElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elementObject : elmArray.getValuesAs(JsonObject.class)) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
				if (elementObject.containsKey("lang")) {
            		attrs.put("xml:lang", elementObject.getString("lang"));
            	}
				if (elementObject.containsKey("type")) {
					if (elementObject.getString("type").equals("olac:language")) {
						attrs.put("olac-language", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:role")) {
						attrs.put("olac-role", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:linguistic-field")) {
						attrs.put("olac-linguistic-field", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:discourse-type")) {
						attrs.put("olac-discourse-type", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:linguistic-type")) {
						attrs.put("olac-linguistic-type", elementObject.getString("code"));
					} else {
						if (DCTERMS_TYPE.contains(elementObject.getString("type"))) {
							attrs.put("dcterms-type", elementObject.getString("type"));
						} else {
							LOGGER.log(Level.WARNING, "DCTerms type is invalide : " + elementObject.getString("type"));
						}
					}
            	}
				builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, tagName, attrs, elementObject.containsKey("value") ? XMLDocument.removeHTMLTag(elementObject.getString("value")) : null);
			}
		}
	}

	public static void writeCmdiOlacMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("xml:lang", multilingualObject.getString("lang"));
		builder.writeStartEndElement(CMDI_OLAC_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}
	
	public List<String> getListHandlesRoot() {
		return listHandlesRoot;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandlesRoot = listHandlesRoot;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
