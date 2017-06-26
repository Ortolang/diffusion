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

public class OlacHandler implements MetadataHandler {

	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {

		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonDoc = jsonReader.readObject();

		try {
			writeOlacDocument(builder);
			
			DublinCoreHandler.writeDcElement("title", jsonDoc, builder);
//			JsonArray multilingualTitles = jsonDoc.getJsonArray("title");
//	        for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
//	        	olac.addDcMultilingualField("title", multilingualTitle.getString("lang"), XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
//	        }
	
//	        JsonArray multilingualDescriptions = jsonDoc.getJsonArray("description");
//	        for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//	        	olac.addDcMultilingualField("description", multilingualTitle.getString("lang"), XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
//	        }
//	
//	        JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
//	        if(corporaLanguages!=null) {
//	            for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
//	            	JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");
//	            	
//	            	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	//            		this.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//	//            		this.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
//	            		olac.addOlacField("language", "olac:language", corporaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
//	            	}
//	            }
//	        }
//	
//	        JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
//	        if(studyLanguages!=null) {
//	            for(JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
//	            	JsonArray multilingualLabels = studyLanguage.getJsonArray("labels");
//	            	
//	            	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	//            		this.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//	//            		this.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
//	            		olac.addOlacField("subject", "olac:language", studyLanguage.getString("id"), label.getString("lang"), label.getString("value"));
//	            	}
//	            }
//	        }
//	        
//	        JsonArray multilingualKeywords = jsonDoc.getJsonArray("keywords");
//	        if(multilingualKeywords!=null) {
//	            for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
//	            	olac.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
//	            }
//	        }
//	
//	        JsonArray contributors = jsonDoc.getJsonArray("contributors");
//	        if(contributors!=null) {
//	            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
//	                JsonArray roles = contributor.getJsonArray("roles");
//	                for(JsonObject role : roles.getValuesAs(JsonObject.class)) {
////	                	JsonObject metaRole = role.getJsonObject("meta_ortolang-referential-json");
//						String roleId = role.getString("id");
//	                    
//						olac.addOlacField("contributor", "olac:role", roleId, OAI_DC.person(contributor));
//	                    
//	                    if("author".equals(roleId)) {
//	                    	olac.addDcField("creator", OAI_DC.person(contributor));
//	                    }
//	                }
//	            }
//	        }
//	
//	        JsonArray producers = jsonDoc.getJsonArray("producers");
//	        if(producers!=null) {
//	        	for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
////	            	JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");
//	            	
//	            	if(producer.containsKey("fullname")) {
//	            		olac.addDcField("publisher", producer.getString("fullname"));
//	            	}
//	            }
//	        }
//	
//	        JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
//	        if(sponsors!=null) {
//	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
////	            	JsonObject metaOrganization = sponsor.getJsonObject("meta_ortolang-referential-json");
//	            	
//	            	if(sponsor.containsKey("fullname")) {
//	            		olac.addOlacField("contributor", "olac:role", "sponsor", sponsor.getString("fullname"));
//	            	}
//	            }
//	        }
//	        
//	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
//	        if(statusOfUse!=null) {
//	        	String idStatusOfUse = statusOfUse.getString("id");
//	        	olac.addDcField("rights", idStatusOfUse);
//	            
//	            JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
//	        	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	        		olac.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
//	        	}
//	        }
//	        JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
//	        if(conditionsOfUse!=null) {
//	        	for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
//	        		olac.addDcMultilingualField("rights", label.getString("lang"), XMLDocument.removeHTMLTag(label.getString("value")));
//	        	}
//	        }
//	        
//	        JsonObject license = jsonDoc.getJsonObject("license");
//	        if(license!=null) {
//	        	if(license.containsKey("label")) {
//	        		olac.addDctermsMultilingualField("license", "fr", license.getString("label"));
//	        	}
//	        }
//	
//	        JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
//	        if(linguisticDataType!=null) {
//	        	olac.addOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
//	        }
//	        JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
//	        if(discourseTypes!=null) {
//	            for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
//	            	olac.addOlacField("type", "olac:discourse-type", discourseType.getString());
//	            }
//	        }
//	        JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
//	        if(linguisticSubjects!=null) {
//	            for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
//	            	olac.addOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
//	            }
//	        }
//	
//	        JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
//	        if(bibligraphicCitations!=null) {
//	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
//	            	olac.addDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
//	            }
//	        }
//	        JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
//	        JsonString creationDate = jsonDoc.getJsonString("originDate");
//	        if(creationDate!=null) {
//	        	olac.addDcField("date", creationDate.getString());
//	          //TODO check date validation and convert
//	            olac.addDctermsField("temporal", "dcterms:W3CDTF", creationDate.getString());
//	        } else {
//	            if(publicationDate!=null) {
//	            	olac.addDcField("date", publicationDate.getString());
//	            }
//	        }
//	        JsonNumber lastModificationDate = doc.getJsonNumber("lastModificationDate");
//	        Long longTimestamp = lastModificationDate.longValue();
//	        Date datestamp = new Date(longTimestamp);
//	        olac.addDctermsField("modified", "dcterms:W3CDTF", w3cdtf.format(datestamp));
			
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
    
}
