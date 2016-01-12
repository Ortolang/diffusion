package fr.ortolang.diffusion.api.oaipmh.format;

import java.io.StringReader;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

public class OLAC extends OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OLAC.class.getName());

    public OLAC() {
        super();
        
        header = "<olac:olac xmlns:olac=\"http://www.language-archives.org/OLAC/1.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.language-archives.org/OLAC/1.1/ http://www.language-archives.org/OLAC/1.1/olac.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd\">";
        footer = "</olac:olac>";
    }

    public void addDctermsField(String name, String xsitype, String value) {
    	fields.add(XMLElement.createDctermsElement(name, value).withAttribute("xsi:type", xsitype));
    }

    public void addDctermsMultilingualField(String name, String lang, String value) {
    	fields.add(XMLElement.createDctermsElement(name, value).withAttribute("xml:lang", lang));
    }
    
    public void addOlacField(String name, String xsitype, String olaccode) {
    	addOlacField(name, xsitype, olaccode, null);
    }

    public void addOlacField(String name, String xsitype, String olaccode, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode));
    }

    public void addOlacField(String name, String xsitype, String olaccode, String lang, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode).withAttribute("xml:lang", lang));
    }

    public static OLAC valueOf(JsonObject doc) {
    	OLAC olac = new OLAC();

        // Identifier
        //TODO Mettre le handle

        // Différence avec OAI_DC ?
        // TODO : Contributor OLAC avec xsi:type + olac:code
        // Subject olac:code
        // TODO Language olac:code + code ISO
        
        // Plus
        // dcterms provenance ? ORTOLANG ?

        try {
    		JsonString metaString = doc.getJsonString("meta_ortolang-item-json");
        	
        	StringReader reader = new StringReader(metaString.getString());
            JsonReader jsonReader = Json.createReader(reader);
            JsonObject meta = jsonReader.readObject();

            try {
	            JsonArray multilingualTitles = meta.getJsonArray("title");
	            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
	            	olac.addDcMultilingualField("title", multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
	            }

	            JsonArray multilingualDescriptions = meta.getJsonArray("description");
	            for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
	            	String nohtml = multilingualTitle.getString("value").toString().replaceAll("\\<.*?>","").replaceAll("\\&nbsp;"," ").replaceAll("\\&","");
	            	olac.addDcMultilingualField("description", multilingualTitle.getString("lang"), nohtml);
	            }

	            JsonArray corporaLanguages = meta.getJsonArray("corporaLanguages");
	            if(corporaLanguages!=null) {
	                for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
	                	JsonObject metaLanguage = corporaLanguage.getJsonObject("meta_ortolang-referentiel-json");
	                	JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");
	                	
	                	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	                		olac.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//	                		olac.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
		                	olac.addOlacField("language", "olac:language", metaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
	                	}
	                }
	            }
	            
	            JsonArray multilingualKeywords = meta.getJsonArray("keywords");
	            if(multilingualKeywords!=null) {
	                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
	                	olac.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
	                }
	            }
	
//	            JsonArray contributors = doc.getJsonArray("contributors");
//	            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
//	                JsonArray roles = contributor.getJsonArray("role");
//	                for(JsonString role : roles.getValuesAs(JsonString.class)) {
//	                    if(role.getString().equals("producer")) {
//	                        JsonObject entityContributor = contributor.getJsonObject("entity");
//	                        String fullname = entityContributor.getString("fullname");
//	                        olac.addDcField("publisher", fullname);
//	                    } else {
//	                    	olac.addOlacField("contributor", "olac:role", role.getString(), contributor(contributor));
//	                    }
//	                    
//	                    if(role.getString().equals("author")) {
//	                    	olac.addDcField("creator", creator(contributor));
//	                    }
//	                }
//	            }

	            JsonArray producers = meta.getJsonArray("producers");
	            if(producers!=null) {
	            	for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
		            	JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referentiel-json");
		            	
		            	if(metaOrganization.containsKey("fullname")) {
		            		olac.addDcField("publisher", metaOrganization.getString("fullname"));
		            	}
		            }
	            }
	            
	            JsonObject statusOfUse = meta.getJsonObject("statusOfUse");
	            if(statusOfUse!=null) {
	            	JsonObject metaStatusOfUse = statusOfUse.getJsonObject("meta_ortolang-referentiel-json");
	            	String idStatusOfUse = metaStatusOfUse.getString("id");
	            	olac.addDcField("rights", idStatusOfUse);
	                
	                JsonArray multilingualLabels = metaStatusOfUse.getJsonArray("labels");
                	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                		olac.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
                	}
	            }
	            JsonArray conditionsOfUse = meta.getJsonArray("conditionsOfUse");
	            if(conditionsOfUse!=null) {
	            	for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
                		olac.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
                	}
	            }
	            
	            JsonObject license = meta.getJsonObject("license");
	            if(license!=null) {
	            	JsonObject metaLicense = license.getJsonObject("meta_ortolang-referentiel-json");
	            	if(metaLicense!=null) {
	            		olac.addDctermsMultilingualField("license", "fr", metaLicense.getString("label"));
	            	}
	            }
	
	            JsonString linguisticDataType = meta.getJsonString("linguisticDataType");
	            if(linguisticDataType!=null) {
	                olac.addOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
	            }
	            JsonArray discourseTypes = meta.getJsonArray("discourseTypes");
	            if(discourseTypes!=null) {
	                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
	                    olac.addOlacField("type", "olac:discourse-type", discourseType.getString());
	                }
	            }
	            JsonArray linguisticSubjects = meta.getJsonArray("linguisticSubjects");
	            if(linguisticSubjects!=null) {
	                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
	                    olac.addOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
	                }
	            }
	
	            JsonArray bibligraphicCitations = meta.getJsonArray("bibliographicCitation");
	            if(bibligraphicCitations!=null) {
		            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
		                olac.addDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
		            }
	            }
	            JsonString publicationDate = meta.getJsonString("publicationDate");
	            JsonString creationDate = meta.getJsonString("originDate");
	            if(creationDate!=null) {
	                olac.addDcField("date", creationDate.getString());
	              //TODO check date validation and convert
	                olac.addDctermsField("temporal", "dcterms:W3CDTF", creationDate.getString());
	            } else {
	                if(publicationDate!=null) {
	                    olac.addDcField("date", publicationDate.getString());
	                }
	            }
//	            if(publicationDate!=null) {
	                //TODO get created date if publicationDate not set
//	                olac.addDctermsField("created", "dcterms:W3CDTF", publicationDate.getString());
//	            }
	            JsonNumber lastModificationDate = doc.getJsonNumber("lastModificationDate");
	            Long longTimestamp = Long.valueOf(lastModificationDate.longValue());
	            Date datestamp = new Date(longTimestamp);
	            olac.addDctermsField("modified", "dcterms:W3CDTF", w3cdtf.format(datestamp));
            
            } catch(NullPointerException | ClassCastException | NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Cannot parse JSON property from meta_ortolang-item-json", e);
            } finally {
                jsonReader.close();
                reader.close();
            }
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
    	return olac;
    }
}
