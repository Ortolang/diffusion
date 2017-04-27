package fr.ortolang.diffusion.oai.format;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

public class OAI_DCFactory {

    private static final Logger LOGGER = Logger.getLogger(OAI_DCFactory.class.getName());

	public static OAI_DC buildFromItem(String item) {
		OAI_DC oai_dc = new OAI_DC();
		
		StringReader reader = new StringReader(item);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        
        try {
        	JsonArray multilingualTitles = jsonDoc.getJsonArray("title");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
                oai_dc.addDcMultilingualField("title", 
                		multilingualTitle.getString("lang"), 
                		XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
            }

            JsonArray multilingualDescriptions = jsonDoc.getJsonArray("description");
            for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
                oai_dc.addDcMultilingualField("description", 
                		multilingualTitle.getString("lang"), 
                		XMLDocument.removeHTMLTag(multilingualTitle.getString("value")));
            }

            JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
                    JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

                    for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                    	oai_dc.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
                    	oai_dc.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
                    }
                    oai_dc.addDcField("language", corporaLanguage.getString("id"));
                }
            }

            JsonArray multilingualKeywords = jsonDoc.getJsonArray("keywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                	oai_dc.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }

            JsonArray producers = jsonDoc.getJsonArray("producers");
            if(producers!=null) {
                for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
//                    JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");

                    if(producer.containsKey("fullname")) {
                    	oai_dc.addDcField("publisher", producer.getString("fullname"));
                    }
                }
            }

            JsonArray contributors = jsonDoc.getJsonArray("contributors");
            if(contributors!=null) {
                for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                    JsonArray roles = contributor.getJsonArray("roles");
                    for(JsonObject role : roles.getValuesAs(JsonObject.class)) {
//                        JsonObject metaRole = role.getJsonObject("meta_ortolang-referential-json");
                        String roleId = role.getString("id");
                        oai_dc.addDcField("contributor", OAI_DC.person(contributor)+" ("+roleId+")");

                        if("author".equals(roleId)) {
                        	oai_dc.addDcField("creator", OAI_DC.person(contributor));
                        }
                    }
                }
            }

            JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
            if(statusOfUse!=null) {
                String idStatusOfUse = statusOfUse.getString("id");
                oai_dc.addDcField("rights", idStatusOfUse);

                JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
                for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                	oai_dc.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
                }
            }
            JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
            if(conditionsOfUse!=null) {
                for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
                	oai_dc.addDcMultilingualField("rights", label.getString("lang"), XMLDocument.removeHTMLTag(label.getString("value")));
                }
            }

            JsonObject license = jsonDoc.getJsonObject("license");
            if(license!=null) {
                if(license!=null) {
                	oai_dc.addDcMultilingualField("rights", "fr", license.getString("label"));
                }
            }

            JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                	oai_dc.addDcField("subject", "linguistic field: "+linguisticSubject.getString());
                }
            }
            JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
            if(linguisticDataType!=null) {
            	oai_dc.addDcField("type", "linguistic-type: "+linguisticDataType.getString());
            }
            JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                	oai_dc.addDcField("type", "discourse-type: "+discourseType.getString());
                }
            }
            JsonString creationDate = jsonDoc.getJsonString("originDate");
            if(creationDate!=null) {
            	oai_dc.addDcField("date", creationDate.getString());
            } else {
                JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
                if(publicationDate!=null) {
                	oai_dc.addDcField("date", publicationDate.getString());
                }
            }
	
        } catch(Exception e) {
        	LOGGER.log(Level.SEVERE, "unable to build OAI_DC", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
        
        return oai_dc;
	}
	

	public static OAI_DC buildFromJson(String json) {
		OAI_DC oai_dc = new OAI_DC();
		StringReader reader = new StringReader(json);
        JsonReader jsonReader = Json.createReader(reader);
        
        try {
        	JsonObject jsonDoc = jsonReader.readObject();
        	// Converts elements from OLAC to DC based on OLAC-to-OAI_DC crosswalk [http://www.language-archives.org/NOTE/olac_display.html]
        	if (jsonDoc.containsKey("type")) {
        		JsonArray elmArray = jsonDoc.getJsonArray("type");
                for(JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
                	if (elm.containsKey("type")) {
                		String xsitype = elm.getString("type");
                		if ("olac:linguistic-type".equals(xsitype) && elm.containsKey("code")) {
                    		// Rules 1 & 2
                			String value = "Linguistic type:" + elm.getString("code").replaceAll("_", " ");
                			oai_dc.addDcField("type", value);
                		} else if ("olac:discourse-type".equals(xsitype)) {
                    		// Rules 1 & 3
                			String value = "Discourse type:" + elm.getString("code").replaceAll("_", " ");
                			oai_dc.addDcField("description", value);
                		}
                	}
                }
        	}
        	if (jsonDoc.containsKey("subject")) {
        		JsonArray elmArray = jsonDoc.getJsonArray("subject");
                for(JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
                	if (elm.containsKey("type")) {
                		String xsitype = elm.getString("type");
                		if ("olac:discourse-type".equals(xsitype) && elm.containsKey("code")) {
                    		// Rules 1 & 2
                			String value = "Discourse type:" + elm.getString("code").replaceAll("_", " ");
                			oai_dc.addDcField("subject", value);
                		} else if ("olac:linguistic-field".equals(xsitype)) {
                    		// Rules 1
                			String value = elm.getString("code").replaceAll("_", " ");
                			oai_dc.addDcField("subject", value);
                		}
                	}
                }
        	}
    		// Rules 4
        	if (jsonDoc.containsKey("contributor")) {
        		JsonArray elmArray = jsonDoc.getJsonArray("contributor");
        		for(JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
        			if (elm.containsKey("code") && "author".equals(elm.getString("code")) && elm.containsKey("value")) {
        				oai_dc.addDcField("creator", elm.getString("value"));
        				//TODO remove contributor
        			}
        		}
        	}
    		//TODO Rules 5
    		// Rules 6
//        	if (jsonDoc.containsKey("date")) {
//        		remainsDateElement("date", jsonDoc);
//        	} else {
        		for (String dateElementName : OLAC.DATE_ELEMENTS) {
        			if (jsonDoc.containsKey(dateElementName)) { 
            			JsonArray elmArray = jsonDoc.getJsonArray(dateElementName);
                		for(JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
                			if (elm.containsKey("value")) {
                				oai_dc.addDcField("date", elm.getString("value"));
                			}
                		}
                		purgeDateElements(jsonDoc);
                		break;
            		}
        		}
//        	}
        	// otherwise add DC elements
        	for(String elm : DCXMLDocument.DC_ELEMENTS) {
        		oai_dc.addDCElement(elm, jsonDoc);
        	}
        	// and convert DCTERMS to DC
        	for(Map.Entry<List<String>, String> elm : OLAC.OLAC_TO_DC_ELEMENTS.entrySet()) {
        		for(String olacElement : elm.getKey()) {
        			oai_dc.addDCElement(olacElement, jsonDoc, elm.getValue());
        		}
        	}
        } catch(Exception e) {
        	LOGGER.log(Level.SEVERE, "unable to build OAI_DC from json", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
        
        return oai_dc;
    }

	private static void purgeDateElements(JsonObject jsonDoc) {
		for(String elmName : OLAC.DATE_ELEMENTS) {
			if (jsonDoc.containsKey(elmName)) {
				jsonDoc.remove(elmName);
			}
		}
	}
}
