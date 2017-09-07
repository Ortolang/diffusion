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
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class OlacHandler implements MetadataHandler {

    private static final Logger LOGGER = Logger.getLogger(OlacHandler.class.getName());

	private List<String> listHandles;
	
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {

		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonDoc = jsonReader.readObject();

		try {
			writeOlacDocument(builder);

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
			
			DublinCoreHandler.writeDcElement("title", jsonDoc, builder);
			DublinCoreHandler.writeDcElement("description", jsonDoc, builder);
			DublinCoreHandler.writeDcElement("keywords", jsonDoc, "subject", builder);

			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						DublinCoreHandler.writeDcMultilingualElement("subject", label, builder);
//						DublinCoreHandler.writeDcMultilingualElement("language", label, builder);
						writeOlacElement("language", "olac:language", corporaLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
				}
			}

			JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
			if (studyLanguages != null) {
				for (JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = studyLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						DublinCoreHandler.writeDcMultilingualElement("subject", label, builder);
//						DublinCoreHandler.writeDcMultilingualElement("language", label, builder);
						writeOlacElement("subject", "olac:language", studyLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
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
						writeOlacElement("contributor", "olac:role", roleId, null, Constant.person(contributor), builder);
	                    
	                    if("author".equals(roleId)) {
	                    	builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "creator", Constant.person(contributor));
	                    }
					}
				}
			}

	        JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
	        if(sponsors!=null) {
	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
	            	
	            	if(sponsor.containsKey("fullname")) {
	            		writeOlacElement("contributor", "olac:role", "sponsor", null, sponsor.getString("fullname"), builder);
	            	}
	            }
	        }
	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
					DublinCoreHandler.writeDcMultilingualElement("rights", label, builder);
				}
			}
			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
			if (conditionsOfUse != null) {
				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
					DublinCoreHandler.writeDcMultilingualElement("rights", label, builder);
				}
			}
			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null) {
				if (license != null) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", "fr");
					builder.writeStartEndElement(Constant.DCTERMS_NAMESPACE_PREFIX, "license", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
				}
			}
			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					writeOlacElement("subject", "olac:linguistic-field", linguisticSubject.getString(), null, null, builder);
				}
			}
			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				writeOlacElement("type", "olac:linguistic-type", linguisticDataType.getString(), null, null, builder);
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					writeOlacElement("type", "olac:discourse-type", discourseType.getString(), null, null, builder);
				}
			}
	        JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
	        if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	            	XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", multilingualBibliographicCitation.getString("lang"));
					builder.writeStartEndElement(Constant.DCTERMS_NAMESPACE_PREFIX, "bibliographicCitation", attrs, XMLDocument.removeHTMLTag(multilingualBibliographicCitation.getString("value")));
	            }
	        }
	        JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
	        JsonString creationDate = jsonDoc.getJsonString("originDate");
	        if(creationDate!=null) {
	        	builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "date", creationDate.getString());
	          //TODO check date validation and convert
	            XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("xsi:type", "dcterms:W3CDTF");
				builder.writeStartEndElement(Constant.DCTERMS_NAMESPACE_PREFIX, "temporal", attrs, creationDate.getString());
	        } else {
	            if(publicationDate!=null) {
		        	builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, "date", publicationDate.getString());
	            }
	        }
			
        	builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build OLAC metadata format", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
//		OLAC olac = new OLAC();
		StringReader reader = new StringReader(json);
        JsonReader jsonReader = Json.createReader(reader);
        
        try {
        	writeOlacDocument(builder);
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
			
        	// DCTerms elements
//        	Constant.DCTERMS_ELEMENTS.stream().forEach(elm -> writeDctermsElement(elm, jsonDoc, builder));
        	for(String dcterms : Constant.DCTERMS_ELEMENTS) {
        		writeDctermsElement(dcterms, jsonDoc, builder);
        	}
        	// Dublin Core elements with OLAC attributes
//        	Constant.DC_ELEMENTS.stream().forEach(elm -> writeOlacElement(elm, jsonDoc, builder));
        	for(String dc : Constant.DC_ELEMENTS) {
        		writeOlacElement(dc, jsonDoc, builder);
        	}
        	
        	builder.writeEndDocument();
        } catch(Exception e) {
        	throw new MetadataHandlerException("unable to build OLAC metadata format from json", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
	}

	public static void writeOlacDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(Constant.OLAC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.OLAC_NAMESPACE_URI, Constant.OLAC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.DC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.DC_NAMESPACE_URI, Constant.DC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.DCTERMS_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.DCTERMS_NAMESPACE_URI, Constant.DCTERMS_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		builder.writeStartDocument(Constant.OLAC_NAMESPACE_PREFIX, Constant.OLAC_ELEMENT, null);
	}

    public void writeDctermsElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
    	if (meta.containsKey(elementName)) {
    		JsonArray elementArray = meta.getJsonArray(elementName);
    		for(JsonObject elementObject : elementArray.getValuesAs(JsonObject.class)) {
//    			XMLElement elementXml = XMLElement.createElement(DCTERMS_NAMESPACE, elementName, XMLDocument.removeHTMLTag(elementObject.getString("value")));
    			XmlDumpAttributes attrs = new XmlDumpAttributes();
    			if (elementObject.containsKey("lang")) {
//    				elementXml.withAttribute("xml:lang", elementObject.getString("lang"));
    				attrs.put("xml:lang", elementObject.getString("lang"));
    			} 
    			if (elementObject.containsKey("type")) {
//    				elementXml.withAttribute("xsi:type", elementObject.getString("type"));
    				attrs.put("xsi:type", elementObject.getString("type"));
    			}
//    			fields.add(elementXml);
    			builder.writeStartEndElement(Constant.DCTERMS_NAMESPACE_PREFIX, elementName, attrs, XMLDocument.removeHTMLTag(elementObject.getString("value")));
    		}
    	}
//    	return this;
    }

    public void writeOlacElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
    	if (meta.containsKey(elementName)) {
    		JsonArray elementArray = meta.getJsonArray(elementName);
            for(JsonObject elementObject : elementArray.getValuesAs(JsonObject.class)) {
//            	XMLElement elementXml = XMLElement.createElement(DC_NAMESPACE, elementName);
            	XmlDumpAttributes attrs = new XmlDumpAttributes();
            	
//    			if (elementObject.containsKey("value")) {
//    				elementXml.setValue(XMLDocument.removeHTMLTag(elementObject.getString("value")));
//    			} 
            	if (elementObject.containsKey("lang")) {
//            		elementXml.withAttribute("xml:lang", elementObject.getString("lang"));
            		attrs.put("xml:lang", elementObject.getString("lang"));
            	} 
            	if (elementObject.containsKey("type")) {
//            		elementXml.withAttribute("xsi:type", elementObject.getString("type"));
            		attrs.put("xsi:type", elementObject.getString("type"));
            	}
            	if (elementObject.containsKey("code")) {
//            		elementXml.withAttribute("olac:code", elementObject.getString("code"));
            		attrs.put("olac:code", elementObject.getString("code"));
            	}
//            	fields.add(elementXml);
            	builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, elementName, attrs, elementObject.containsKey("value") ? XMLDocument.removeHTMLTag(elementObject.getString("value")) : null);
            }
    	}
//    	return this;
    }

    public void writeOlacElement(String elementName, String xsitype, String olaccode, String lang, String value, MetadataBuilder builder) throws MetadataBuilderException {
    	XmlDumpAttributes attrs = new XmlDumpAttributes();
    	if (xsitype!=null) {
    		attrs.put("xsi:type", xsitype);
    	}
    	if (olaccode!=null) {
    		attrs.put("olac:code", olaccode);
    	}
    	if (lang!=null) {
    		attrs.put("xml:lang", lang);
    	}
    	builder.writeStartEndElement(Constant.DC_NAMESPACE_PREFIX, elementName, attrs, value!=null ? XMLDocument.removeHTMLTag(value) : null);
//    	fields.add(XMLElement.createElement(OLAC_NAMESPACE, name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode).withAttribute("xml:lang", lang));
    }

	public List<String> getListHandlesRoot() {
		return listHandles;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandles = listHandlesRoot;
	}

}
