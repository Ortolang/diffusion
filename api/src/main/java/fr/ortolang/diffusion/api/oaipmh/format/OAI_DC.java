package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());
    
    private String title;
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">");
        
        buffer.append("<dc:title>").append(title).append("</dc:title>");
        
        buffer.append("</oai_dc:dc>");
        return buffer.toString();
    }
    
    public static OAI_DC valueOf(JsonObject doc) {
        OAI_DC oai_dc = new OAI_DC();
        
        try {
            JsonArray multilingualTitles = doc.getJsonArray("title");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
                if(multilingualTitle.getString("lang").equals("fr")) {
                    oai_dc.setTitle(multilingualTitle.getString("value"));
                }
            }
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
        return oai_dc;
    }
}
