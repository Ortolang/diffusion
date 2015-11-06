package fr.ortolang.diffusion.api.oaipmh.format;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

public class OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());
    protected static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");
    
    protected String header;
    protected String footer;
    protected List<XMLElement> fields;
    
    public OAI_DC() {
    	header = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">";
    	footer = "</oai_dc:dc>";
    	fields = new ArrayList<XMLElement>();
    }

    public void addDcField(String name, String value) {
    	fields.add(XMLElement.createDcElement(name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xml:lang", lang));
    }
    
    protected static void writeField(StringBuilder buffer, XMLElement elem) {
    	
    	buffer.append("<").append(elem.getPrefixNamespace()).append(":").append(elem.getName());
    	
    	if(elem.getAttributes()!=null) {
    		for(Map.Entry<String, String> attr : elem.getAttributes().entrySet()) {
    			buffer.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
    		}
    	}
    	
    	if(elem.getValue()!=null) {
    		buffer.append(">").append(elem.getValue()).append("</").append(elem.getPrefixNamespace()).append(":").append(elem.getName()).append(">");
    	} else {
    		buffer.append("/>");
    	}
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append(header);

        for(XMLElement elem : fields) {
        	writeField(buffer, elem);
        }
        
        buffer.append(footer);
        
        return buffer.toString();
    }
    
    public static OAI_DC valueOf(JsonObject doc) {
        OAI_DC oai_dc = new OAI_DC();

        // Identifier
        //TODO Mettre le handle
        //TODO Pictogramme ? Pas exploiter par Isidore
        //TODO ARK
        
        try {
            JsonArray multilingualTitles = doc.getJsonArray("meta_ortolang-item-jsontitle");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
                oai_dc.addDcMultilingualField("title", multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }
//            JsonArray multilingualDescriptions = doc.getJsonArray("meta_ortolang-item-jsondescription");
//            for(JsonObject multilingualDescription : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//                oai_dc.putDescription(multilingualDescription.getString("lang"), multilingualDescription.getString("value"));
//            }
            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                	oai_dc.addDcMultilingualField("subject", "fr", corporaLanguage.getString());
                	oai_dc.addDcMultilingualField("language", "fr", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                	oai_dc.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }
            
            JsonArray contributors = doc.getJsonArray("meta_ortolang-item-jsoncontributors");
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("role");
                for(JsonString role : roles.getValuesAs(JsonString.class)) {
                    if(role.getString().equals("producer")) {
                        JsonObject entityContributor = contributor.getJsonObject("entity");
                        String fullname = entityContributor.getString("fullname");
                        oai_dc.addDcField("publisher", fullname);
                    } else {
                        oai_dc.addDcField("contributor", contributor(contributor, role.getString()));
                    }
                    
                    if(role.getString().equals("author")) {
                        oai_dc.addDcField("creator", creator(contributor));
                    }
                }
            }

            JsonString statusOfUse = doc.getJsonString("meta_ortolang-item-jsonstatusOfUse");
            if(statusOfUse!=null) {
                oai_dc.addDcField("rights", statusOfUse.getString());
            }
            JsonString conditionsOfUse = doc.getJsonString("meta_ortolang-item-jsonconditionsOfUse");
            if(conditionsOfUse!=null) {
                oai_dc.addDcField("rights", conditionsOfUse.getString());
            }

            JsonArray linguisticSubjects = doc.getJsonArray("meta_ortolang-item-jsonlinguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                    oai_dc.addDcField("subject", "linguistic field: "+linguisticSubject.getString());
                }
            }
            JsonString linguisticDataType = doc.getJsonString("meta_ortolang-item-jsonlinguisticDataType");
            if(linguisticDataType!=null) {
                oai_dc.addDcField("type", "linguistic-type: "+linguisticDataType.getString());
            }
            JsonArray discourseTypes = doc.getJsonArray("meta_ortolang-item-jsondiscourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                    oai_dc.addDcField("type", "discourse-type: "+discourseType.getString());
                }
            }
            JsonString creationDate = doc.getJsonString("meta_ortolang-item-jsoncreationDate");
            if(creationDate!=null) {
                oai_dc.addDcField("date", creationDate.getString());
            } else {
                JsonString publicationDate = doc.getJsonString("meta_ortolang-item-jsonpublicationDate");
                if(publicationDate!=null) {
                    oai_dc.addDcField("date", publicationDate.getString());
                }
            }
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
        return oai_dc;
    }

    protected static String contributor(JsonObject contributor) {
    	return contributor(contributor, null);
    }
    
    protected static String contributor(JsonObject contributor, String role) {
        JsonObject entityContributor = contributor.getJsonObject("entity");
        JsonString lastname = entityContributor.getJsonString("lastname");
        JsonString midname = entityContributor.getJsonString("midname");
        JsonString firstname = entityContributor.getJsonString("firstname");
        JsonString title = entityContributor.getJsonString("title");
        JsonObject entityOrganization = entityContributor.getJsonObject("organization");
        JsonString acronym = null;
        if(entityOrganization!=null) {
            acronym = entityOrganization.getJsonString("acronym"); 
        }
        return lastname.getString()+(midname!=null?", "+midname.getString():"")+(firstname!=null?", "+firstname.getString():"")+(title!=null?" "+title.getString():"")+(acronym!=null?", "+acronym.getString():"")+(role!=null?" ("+role+")":"");
    }

    protected static String creator(JsonObject contributor) {
        JsonObject entityContributor = contributor.getJsonObject("entity");
        JsonString lastname = entityContributor.getJsonString("lastname");
        JsonString midname = entityContributor.getJsonString("midname");
        JsonString firstname = entityContributor.getJsonString("firstname");
        JsonString title = entityContributor.getJsonString("title");
        JsonObject entityOrganization = entityContributor.getJsonObject("organization");
        JsonString acronym = null;
        if(entityOrganization!=null) {
            acronym = entityOrganization.getJsonString("acronym"); 
        }
        return lastname.getString()+(midname!=null?", "+midname.getString():"")+(firstname!=null?", "+firstname.getString():"")+(title!=null?" "+title.getString():"")+(acronym!=null?", "+acronym.getString():"");
    }
}
