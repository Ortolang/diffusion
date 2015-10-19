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

public class OLAC extends OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OLAC.class.getName());

    protected Map<String, Map<String, List<String>>> olacFields;
    protected Map<String, Map<String, List<String>>> dctermsMultilingualFields;
    
    public OLAC() {
        olacFields = new HashMap<String, Map<String, List<String>>>();
        dctermsMultilingualFields = new HashMap<String, Map<String, List<String>>>();
    }

    public Optional<Map.Entry<String, Map<String, List<String>>>> getOlacField(String name) {
        return olacFields.entrySet().stream().filter(field -> field.getKey().equals(name))
                .findFirst();
    }
    
    public void setOlacField(String name, String type, String code) {
        Optional<Map.Entry<String, Map<String, List<String>>>> field = getOlacField(name);
        if(field.isPresent()) {
            List<String> values = field.get().getValue().get(type);
            if(values!=null) {
                values.add(code);
            } else {
                values = new ArrayList<String>();
                values.add(code);
                field.get().getValue().put(type, values);
            }
        } else {
            Map<String, List<String>> olacValue = new HashMap<String, List<String>>();
            List<String> values = new ArrayList<String>();
            values.add(code);
            olacValue.put(type, values);
            olacFields.put(name, olacValue);
        }
    }

    protected static void writeOlacCode(StringBuilder buffer, String name, Map<String,List<String>> values) {
        for(Map.Entry<String, List<String>> entry : values.entrySet()) {
            for(String value : entry.getValue()) {
                buffer.append("<dc:").append(name).append(" xsi:type=\"").append(entry.getKey()).append("\" olac:code=\"").append(value).append("\"/>");
            }
        }
    }

    public Optional<Map.Entry<String, Map<String, List<String>>>> getDctermsMultilingualField(String name) {
        return dctermsMultilingualFields.entrySet().stream().filter(field -> field.getKey().equals(name))
                .findFirst();
    }
    
    public void setDctermsMultilingualField(String name, String lang, String value) {
        Optional<Map.Entry<String, Map<String, List<String>>>> field = getDctermsMultilingualField(name);
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
            dctermsMultilingualFields.put(name, multilingualValue);
        }
    }

    protected static void writeDctermsMultilingualValue(StringBuilder buffer, String name, Map<String,List<String>> values) {
        for(Map.Entry<String, List<String>> entry : values.entrySet()) {
            for(String value : entry.getValue()) {
                buffer.append("<dcterms:").append(name).append(" xml:lang=\"").append(entry.getKey()).append("\">").append(value).append("</dcterms:").append(name).append(">");
            }
        }
    }
    
	public String toString() {
		StringBuilder buffer = new StringBuilder();
        
		buffer.append("<olac:olac xmlns:olac=\"http://www.language-archives.org/OLAC/1.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.language-archives.org/OLAC/1.1/ http://www.language-archives.org/OLAC/1.1/olac.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd\">");
        
        for(Map.Entry<String, Map<String, List<String>>> multilingualEntry : multilingualFields.entrySet()) {
            writeMultilingualValue(buffer, multilingualEntry.getKey(), multilingualEntry.getValue());
        }
        for(Map.Entry<String, Map<String, List<String>>> multilingualEntry : dctermsMultilingualFields.entrySet()) {
            writeDctermsMultilingualValue(buffer, multilingualEntry.getKey(), multilingualEntry.getValue());
        }
        for(Map.Entry<String, List<String>> multivalueEntry : multivalueFields.entrySet()) {
            writeMultivalueField(buffer, multivalueEntry.getKey(), multivalueEntry.getValue());
        }
        for(Map.Entry<String, Map<String, List<String>>> olacEntry : olacFields.entrySet()) {
            writeOlacCode(buffer, olacEntry.getKey(), olacEntry.getValue());
        }
        buffer.append("</olac:olac>");
        
        return buffer.toString();
    }
	

    public static OLAC valueOf(JsonObject doc) {
    	OLAC olac = new OLAC();

        // Identifier
        //TODO Mettre le handle
        //TODO Pictogramme ? Pas exploiter par Isidore
        //TODO ARK

        // Différence avec OAI_DC ?
        // Contributor OLAC avec xsi:type + olac:code
        // Source => Provenance
        // Subject olac:code
        // Language olac:code
        
        // Plus
        // dcterms provenance

        try {
            JsonArray multilingualTitles = doc.getJsonArray("meta_ortolang-item-jsontitle");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
                olac.setMultilingualField("title", multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }

            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                    olac.setMultilingualField("subject", "fr", corporaLanguage.getString());
                    olac.setMultilingualField("language", "fr", corporaLanguage.getString());
//                    olac.setOlacField("language", "olac:language", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                    olac.setMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }
            
            JsonString statusOfUse = doc.getJsonString("meta_ortolang-item-jsonstatusOfUse");
            if(statusOfUse!=null) {
                olac.setMultivalueField("rights", statusOfUse.getString());
            }
            JsonString conditionsOfUse = doc.getJsonString("meta_ortolang-item-jsonconditionsOfUse");
            if(conditionsOfUse!=null) {
                olac.setMultivalueField("rights", conditionsOfUse.getString());
            }
            JsonString licenseWebsite = doc.getJsonString("meta_ortolang-item-jsonlicenseWebsite");
            if(licenseWebsite!=null) {
//                olac.setMultivalueField("rights", licenseWebsite.getString());
                //TODO dcterms
            }

            JsonString linguisticDataType = doc.getJsonString("meta_ortolang-item-jsonlinguisticDataType");
            if(linguisticDataType!=null) {
                olac.setOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
            }
            JsonArray discourseTypes = doc.getJsonArray("meta_ortolang-item-jsondiscourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                    olac.setOlacField("type", "olac:discourse-type", discourseType.getString());
                }
            }
            JsonArray linguisticSubjects = doc.getJsonArray("meta_ortolang-item-jsonlinguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                    olac.setOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
                }
            }

            JsonArray bibligraphicCitations = doc.getJsonArray("meta_ortolang-item-jsonbibliographicCitation");
            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
                olac.setDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
            }
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
    	return olac;
    }
}
