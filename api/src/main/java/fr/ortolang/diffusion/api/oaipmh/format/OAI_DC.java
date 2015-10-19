package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

public class OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());

    protected Map<String, List<String>> multivalueFields;
    protected Map<String, Map<String, List<String>>> multilingualFields;
    
    public OAI_DC() {
        multivalueFields = new HashMap<String, List<String>>();
        multilingualFields = new HashMap<String, Map<String, List<String>>>();
    }

    public Optional<Map.Entry<String, List<String>>> getMultivalueField(String name) {
        return multivalueFields.entrySet().stream().filter(field -> field.getKey().equals(name))
                .findFirst();
    }

    public void setMultivalueField(String name, String value) {
        Optional<Map.Entry<String, List<String>>> field = getMultivalueField(name);
        if(field.isPresent()) {
            if(!field.get().getValue().contains(value)) {
                field.get().getValue().add(value);
            }
        } else {
            List<String> newFieldValue = new ArrayList<String>();
            newFieldValue.add(value);
            multivalueFields.put(name, newFieldValue);
        }
    }
    
    public Optional<Map.Entry<String, Map<String, List<String>>>> getMultilingualField(String name) {
        return multilingualFields.entrySet().stream().filter(field -> field.getKey().equals(name))
                .findFirst();
    }
    
    public void setMultilingualField(String name, String lang, String value) {
        Optional<Map.Entry<String, Map<String, List<String>>>> field = getMultilingualField(name);
        if(field.isPresent()) {
            List<String> values = field.get().getValue().get(lang);
            if(values!=null) {
                values.add(value);
            } else {
                values = new ArrayList<String>();
                values.add(value);
                field.get().getValue().put(lang, values);
            }
        } else {
            Map<String, List<String>> multilingualValue = new HashMap<String, List<String>>();
            List<String> values = new ArrayList<String>();
            values.add(value);
            multilingualValue.put(lang, values);
            multilingualFields.put(name, multilingualValue);
        }
    }
    
    protected static void writeMultilingualValue(StringBuilder buffer, String name, Map<String,List<String>> values) {
    	for(Map.Entry<String, List<String>> entry : values.entrySet()) {
    	    for(String value : entry.getValue()) {
    	        buffer.append("<dc:").append(name).append(" xml:lang=\"").append(entry.getKey()).append("\">").append(value).append("</dc:").append(name).append(">");
    	    }
        }
    }
    
    protected static void writeMultivalueField(StringBuilder buffer, String name, List<String> values) {
        for(String value : values) {
            buffer.append("<dc:").append(name).append(">").append(value).append("</dc:").append(name).append(">");
        }
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">");

        for(Map.Entry<String, Map<String, List<String>>> multilingualEntry : multilingualFields.entrySet()) {
            writeMultilingualValue(buffer, multilingualEntry.getKey(), multilingualEntry.getValue());
        }
        for(Map.Entry<String, List<String>> multivalueEntry : multivalueFields.entrySet()) {
            writeMultivalueField(buffer, multivalueEntry.getKey(), multivalueEntry.getValue());
        }
        buffer.append("</oai_dc:dc>");
        
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
                oai_dc.setMultilingualField("title", multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }
//            JsonArray multilingualDescriptions = doc.getJsonArray("meta_ortolang-item-jsondescription");
//            for(JsonObject multilingualDescription : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//                oai_dc.putDescription(multilingualDescription.getString("lang"), multilingualDescription.getString("value"));
//            }
            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                    oai_dc.setMultilingualField("subject", "fr", corporaLanguage.getString());
                    oai_dc.setMultilingualField("language", "fr", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                    oai_dc.setMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }
            
            JsonArray contributors = doc.getJsonArray("meta_ortolang-item-jsoncontributors");
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("role");
                for(JsonString role : roles.getValuesAs(JsonString.class)) {
                    if(role.getString().equals("producer")) {
                        JsonObject entityContributor = contributor.getJsonObject("entity");
                        String fullname = entityContributor.getString("fullname");
                        oai_dc.setMultivalueField("publisher", fullname);
                    } else {
                        oai_dc.setMultivalueField("contributor", contributor(contributor, role.getString()));
                    }
                    
                    if(role.getString().equals("author")) {
                        oai_dc.setMultivalueField("creator", creator(contributor));
                    }
                }
            }

            JsonString statusOfUse = doc.getJsonString("meta_ortolang-item-jsonstatusOfUse");
            if(statusOfUse!=null) {
                oai_dc.setMultivalueField("rights", statusOfUse.getString());
            }
            JsonString conditionsOfUse = doc.getJsonString("meta_ortolang-item-jsonconditionsOfUse");
            if(conditionsOfUse!=null) {
                oai_dc.setMultivalueField("rights", conditionsOfUse.getString());
            }

            JsonArray linguisticSubjects = doc.getJsonArray("meta_ortolang-item-jsonlinguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                    oai_dc.setMultivalueField("subject", "linguistic field: "+linguisticSubject.getString());
                }
            }
            JsonString linguisticDataType = doc.getJsonString("meta_ortolang-item-jsonlinguisticDataType");
            if(linguisticDataType!=null) {
                oai_dc.setMultivalueField("type", "linguistic-type: "+linguisticDataType.getString());
            }
            JsonArray discourseTypes = doc.getJsonArray("meta_ortolang-item-jsondiscourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                    oai_dc.setMultivalueField("type", "discourse-type: "+discourseType.getString());
                }
            }
            
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
        return oai_dc;
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
        return lastname.getString()+(midname!=null?", "+midname.getString():"")+(firstname!=null?", "+firstname.getString():"")+(title!=null?" "+title.getString():"")+(acronym!=null?", "+acronym.getString():"")+" ("+role+")";
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
