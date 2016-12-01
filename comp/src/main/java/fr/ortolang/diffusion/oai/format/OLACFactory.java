package fr.ortolang.diffusion.oai.format;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

public class OLACFactory {

    private static final Logger LOGGER = Logger.getLogger(OLACFactory.class.getName());
    
	public static OLAC buildFromItem(String item) {
		OLAC olac = new OLAC();
		
		StringReader reader = new StringReader(item);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        
		try {
	        JsonArray multilingualTitles = jsonDoc.getJsonArray("title");
	        for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
	        	olac.addDcMultilingualField("title", multilingualTitle.getString("lang"), XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
	        }
	
	        JsonArray multilingualDescriptions = jsonDoc.getJsonArray("description");
	        for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
	        	olac.addDcMultilingualField("description", multilingualTitle.getString("lang"), XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
	        }
	
	        JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
	        if(corporaLanguages!=null) {
	            for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
	            	JsonObject metaLanguage = corporaLanguage.getJsonObject("meta_ortolang-referential-json");
	            	JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");
	            	
	            	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
	//            		this.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
	//            		this.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
	            		olac.addOlacField("language", "olac:language", metaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
	            	}
	            }
	        }
	
	        JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
	        if(studyLanguages!=null) {
	            for(JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
	            	JsonObject metaLanguage = studyLanguage.getJsonObject("meta_ortolang-referential-json");
	            	JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");
	            	
	            	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
	//            		this.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
	//            		this.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
	            		olac.addOlacField("subject", "olac:language", metaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
	            	}
	            }
	        }
	        
	        JsonArray multilingualKeywords = jsonDoc.getJsonArray("keywords");
	        if(multilingualKeywords!=null) {
	            for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
	            	olac.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
	            }
	        }
	
	        JsonArray contributors = jsonDoc.getJsonArray("contributors");
	        if(contributors!=null) {
	            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
	                JsonArray roles = contributor.getJsonArray("roles");
	                for(JsonObject role : roles.getValuesAs(JsonObject.class)) {
	                	JsonObject metaRole = role.getJsonObject("meta_ortolang-referential-json");
						String roleId = metaRole.getString("id");
	                    
						olac.addOlacField("contributor", "olac:role", roleId, OAI_DC.person(contributor));
	                    
	                    if("author".equals(roleId)) {
	                    	olac.addDcField("creator", OAI_DC.person(contributor));
	                    }
	                }
	            }
	        }
	
	        JsonArray producers = jsonDoc.getJsonArray("producers");
	        if(producers!=null) {
	        	for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
	            	JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");
	            	
	            	if(metaOrganization.containsKey("fullname")) {
	            		olac.addDcField("publisher", metaOrganization.getString("fullname"));
	            	}
	            }
	        }
	
	        JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
	        if(sponsors!=null) {
	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
	            	JsonObject metaOrganization = sponsor.getJsonObject("meta_ortolang-referential-json");
	            	
	            	if(metaOrganization.containsKey("fullname")) {
	            		olac.addOlacField("contributor", "olac:role", "sponsor", metaOrganization.getString("fullname"));
	            	}
	            }
	        }
	        
	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
	        if(statusOfUse!=null) {
	        	JsonObject metaStatusOfUse = statusOfUse.getJsonObject("meta_ortolang-referential-json");
	        	String idStatusOfUse = metaStatusOfUse.getString("id");
	        	olac.addDcField("rights", idStatusOfUse);
	            
	            JsonArray multilingualLabels = metaStatusOfUse.getJsonArray("labels");
	        	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
	        		olac.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
	        	}
	        }
	        JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
	        if(conditionsOfUse!=null) {
	        	for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
	        		olac.addDcMultilingualField("rights", label.getString("lang"), XMLDocument.removeHTMLTag(label.getString("value")));
	        	}
	        }
	        
	        JsonObject license = jsonDoc.getJsonObject("license");
	        if(license!=null) {
	        	JsonObject metaLicense = license.getJsonObject("meta_ortolang-referential-json");
	        	if(metaLicense!=null) {
	        		olac.addDctermsMultilingualField("license", "fr", metaLicense.getString("label"));
	        	}
	        }
	
	        JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
	        if(linguisticDataType!=null) {
	        	olac.addOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
	        }
	        JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
	        if(discourseTypes!=null) {
	            for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
	            	olac.addOlacField("type", "olac:discourse-type", discourseType.getString());
	            }
	        }
	        JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
	        if(linguisticSubjects!=null) {
	            for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
	            	olac.addOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
	            }
	        }
	
	        JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
	        if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	            	olac.addDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
	            }
	        }
	        JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
	        JsonString creationDate = jsonDoc.getJsonString("originDate");
	        if(creationDate!=null) {
	        	olac.addDcField("date", creationDate.getString());
	          //TODO check date validation and convert
	            olac.addDctermsField("temporal", "dcterms:W3CDTF", creationDate.getString());
	        } else {
	            if(publicationDate!=null) {
	            	olac.addDcField("date", publicationDate.getString());
	            }
	        }
//	        JsonNumber lastModificationDate = doc.getJsonNumber("lastModificationDate");
//	        Long longTimestamp = lastModificationDate.longValue();
//	        Date datestamp = new Date(longTimestamp);
//	        olac.addDctermsField("modified", "dcterms:W3CDTF", w3cdtf.format(datestamp));
		} catch(Exception e) {
        	LOGGER.log(Level.SEVERE, "unable to build OLAC from item", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
		
        return olac;
	}
	
	public static OLAC buildFromJson(String json) {
		OLAC olac = new OLAC();
		StringReader reader = new StringReader(json);
        JsonReader jsonReader = Json.createReader(reader);
        
        try {
        	JsonObject jsonDoc = jsonReader.readObject();
        	
        	// DCTerms elements
        	OLAC.DCTERMS_ELEMENTS.stream().forEach(elm -> olac.addDctermsElement(elm, jsonDoc));
        	// Dublin Core elements with OLAC attributes
        	DCXMLDocument.DC_ELEMENTS.stream().forEach(elm -> olac.addOlacElement(elm, jsonDoc));
        } catch(Exception e) {
        	LOGGER.log(Level.SEVERE, "unable to build OLAC from json", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
        
        return olac;
    }
}
