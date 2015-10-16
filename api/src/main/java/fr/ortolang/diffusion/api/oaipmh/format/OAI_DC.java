package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.apache.commons.lang.StringEscapeUtils;

public class OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());
    
    private Map<String,String> title;
    private Map<String,String> description;
    private Map<String,String> subject;
    private Map<String,String> language;
    private List<String> publisher;
    private List<String> contributor;
    private List<String> creator;
    
    public OAI_DC() {
        title = new HashMap<String, String>();
        description = new HashMap<String, String>();
        subject = new HashMap<String, String>();
        language = new HashMap<String, String>();
        publisher = new ArrayList<String>();
        contributor = new ArrayList<String>();
        creator = new ArrayList<String>();
    }
    
    public Map<String,String> getTitle() {
        return title;
    }

    public void putTitle(String lang, String value) {
        this.title.put(lang, value);
    }

    public Map<String,String> getDescription() {
        return description;
    }

    public void putDescription(String lang, String value) {
        this.description.put(lang, value);
    }

    public Map<String,String> getSubject() {
        return subject;
    }

    public void putSubject(String lang, String value) {
        this.subject.put(lang, value);
    }

    public Map<String,String> getLanguage() {
        return language;
    }

    public void putLanguage(String lang, String value) {
        this.language.put(lang, value);
    }

    public List<String> getPublisher() {
        return publisher;
    }

    public void putPublisher(String publisher) {
        this.publisher.add(publisher);
    }

    public List<String> getContributor() {
        return contributor;
    }

    public void putContributor(String contributor) {
        this.contributor.add(contributor);
    }

    public void putCreator(String creator) {
        this.creator.add(creator);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">");
        
        for(Map.Entry<String, String> entry : title.entrySet()) {
            buffer.append("<dc:title xml:lang=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</dc:title>");
        }

        for(Map.Entry<String, String> entry : description.entrySet()) {
            buffer.append("<dc:description xml:lang=\"").append(entry.getKey()).append("\">").append(StringEscapeUtils.escapeHtml(entry.getValue())).append("</dc:description>");
        }

        for(Map.Entry<String, String> entry : subject.entrySet()) {
            buffer.append("<dc:subject xml:lang=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</dc:subject>");
        }

        for(Map.Entry<String, String> entry : language.entrySet()) {
            buffer.append("<dc:language xml:lang=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</dc:language>");
        }
        
        for(String value : publisher) {
            buffer.append("<dc:publisher>").append(value).append("</dc:publisher>");
        }

        for(String value : contributor) {
            buffer.append("<dc:contributor>").append(value).append("</dc:contributor>");
        }

        for(String value : creator) {
            buffer.append("<dc:creator>").append(value).append("</dc:creator>");
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
                oai_dc.putTitle(multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }
//            JsonArray multilingualDescriptions = doc.getJsonArray("meta_ortolang-item-jsondescription");
//            for(JsonObject multilingualDescription : multilingualDescriptions.getValuesAs(JsonObject.class)) {
//                oai_dc.putDescription(multilingualDescription.getString("lang"), multilingualDescription.getString("value"));
//            }
            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                    oai_dc.putSubject("fr", corporaLanguage.getString());
                    oai_dc.putLanguage("fr", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                    oai_dc.putSubject(multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }
            
            JsonArray contributors = doc.getJsonArray("meta_ortolang-item-jsoncontributors");
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("role");
                for(JsonString role : roles.getValuesAs(JsonString.class)) {
                    if(role.getString().equals("producer")) {
                        JsonObject entityContributor = contributor.getJsonObject("entity");
                        String fullname = entityContributor.getString("fullname");
                        oai_dc.putPublisher(fullname);
                    } else {
                        oai_dc.putContributor(contributor(contributor, role.getString()));
                    }
                    
                    if(role.getString().equals("author")) {
                        oai_dc.putCreator(creator(contributor));
                    }
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
        return lastname+(midname!=null?", "+midname:"")+(firstname!=null?", "+firstname:"")+(title!=null?" "+title:"")+(acronym!=null?", "+acronym:"")+" ("+role+")";
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
        return lastname+(midname!=null?", "+midname:"")+(firstname!=null?", "+firstname:"")+(title!=null?" "+title:"")+(acronym!=null?", "+acronym:"");
    }
}
