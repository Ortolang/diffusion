package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;

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
			writeCmdiOlacElement("description", jsonDoc, builder);
			writeCmdiOlacElement("keywords", jsonDoc, "subject", builder);

			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						writeCmdiOlacElement("subject", label, builder);
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-language", label.getString("lang"));
				    	if (label.containsKey("lang")) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "language", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
//						writeOlacElement("language", "olac:language", corporaLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
				}
			}

			JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
			if (studyLanguages != null) {
				for (JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = studyLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//						writeCmdiOlacElement("subject", label, builder);
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-language", label.getString("id"));
				    	if (label.containsKey("lang")) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
//						writeOlacElement("subject", "olac:language", studyLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
				}
			}

			JsonArray producers = jsonDoc.getJsonArray("producers");
			if (producers != null) {
				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
					if (producer.containsKey("fullname")) {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
					}
				}
			}

			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			if (contributors != null) {
				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
					JsonArray roles = contributor.getJsonArray("roles");
					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
						String roleId = role.getString("id");
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-role", roleId);
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, Constant.person(contributor));
	                    
	                    if("author".equals(roleId)) {
	                    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "creator", Constant.person(contributor));
	                    }
					}
				}
			}

			JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
	        if(sponsors!=null) {
	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
	            	if(sponsor.containsKey("fullname")) {
	            		XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-role", "sponsor");
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, sponsor.getString("fullname"));
	            	}
	            }
	        }

	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}

			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
			if (conditionsOfUse != null) {
				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}
			
			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null) {
				if (license != null) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", "fr");
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "license", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
				}
			}
			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("olac-linguistic-field", linguisticSubject.getString());
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, null);
					
				}
			}
			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("olac-linguistic-type", linguisticDataType.getString());
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("olac-discourse-type", discourseType.getString());
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
				}
			}
			JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
	        if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	            	XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", multilingualBibliographicCitation.getString("lang"));
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "bibliographicCitation", attrs, XMLDocument.removeHTMLTag(multilingualBibliographicCitation.getString("value")));
	            }
	        }
	        JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
	        JsonString creationDate = jsonDoc.getJsonString("originDate");
	        if(creationDate!=null) {
	        	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "date", creationDate.getString());
	          //TODO check date validation and convert
	            XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("dcterms-type", "W3CDTF");
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "temporal", attrs, creationDate.getString());
	        } else {
	            if(publicationDate!=null) {
		        	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "date", publicationDate.getString());
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

    public void writeCmdiOlacElement(String elementName, String xsitype, String olaccode, String lang, String value, MetadataBuilder builder) throws MetadataBuilderException {
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
    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, elementName, attrs, value!=null ? XMLDocument.removeHTMLTag(value) : null);
    }
}
