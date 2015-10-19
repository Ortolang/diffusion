package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

public class OLAC extends OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OLAC.class.getName());
    
	public String toString() {
		StringBuilder buffer = new StringBuilder();
        
		buffer.append("<olac:olac xmlns:olac=\"http://www.language-archives.org/OLAC/1.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.language-archives.org/OLAC/1.1/ http://www.language-archives.org/OLAC/1.1/olac.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd\">");
        
        // Différence avec OAI_DC ?
        // Contributor OLAC avec xsi:type + olac:code
        // Source => Provenance
        // Subject olac:code
        // Language olac:code
        
        // Plus
        // dcterms bibliographicCitation
        // dcterms provenance

        writeMultilingualValue(buffer, "title", title);
        writeMultilingualValue(buffer, "description", description);
        writeMultilingualValue(buffer, "subject", subject);
        writeMultilingualValue(buffer, "language", language);
        writeMultivalueField(buffer, "publisher", publisher);
        writeMultivalueField(buffer, "contributor", contributor);
        writeMultivalueField(buffer, "creator", creator);
        
        buffer.append("</olac:olac>");
        
        return buffer.toString();
    }
	

    public static OLAC valueOf(JsonObject doc) {
    	OLAC olac = new OLAC();

        // Identifier
        //TODO Mettre le handle
        //TODO Pictogramme ? Pas exploiter par Isidore
        //TODO ARK
        
        try {
            JsonArray multilingualTitles = doc.getJsonArray("meta_ortolang-item-jsontitle");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
            	olac.putTitle(multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }
//            JsonArray multilingualDescriptions = doc.getJsonArray("meta_ortolang-item-jsondescription");
//            for(JsonObject multilingualDescription : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//                oai_dc.putDescription(multilingualDescription.getString("lang"), multilingualDescription.getString("value"));
//            }
            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                	olac.putSubject("fr", corporaLanguage.getString());
                	olac.putLanguage("fr", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                	olac.putSubject(multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }
            
            JsonArray contributors = doc.getJsonArray("meta_ortolang-item-jsoncontributors");
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("role");
                for(JsonString role : roles.getValuesAs(JsonString.class)) {
                    if(role.getString().equals("producer")) {
                        JsonObject entityContributor = contributor.getJsonObject("entity");
                        String fullname = entityContributor.getString("fullname");
                        olac.putPublisher(fullname);
                    } else {
                    	olac.putContributor(contributor(contributor, role.getString()));
                    }
                    
                    if(role.getString().equals("author")) {
                    	olac.putCreator(creator(contributor));
                    }
                }
                
            }
            
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
    	return olac;
    }
}
