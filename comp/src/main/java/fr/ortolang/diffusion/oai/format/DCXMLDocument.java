package fr.ortolang.diffusion.oai.format;

import java.util.List;
import java.util.Arrays;

import javax.json.JsonArray;
import javax.json.JsonObject;

public abstract class DCXMLDocument extends XMLDocument {

	public static final String DC_NAMESPACE = "dc";
	public static final List<String> DC_ELEMENTS = Arrays.asList("identifier", "title", "creator", "subject", "description", "publisher", 
			"contributor", "date", "type", "format", "source", "language", "relation", "coverage", "rights");
	
    public void addDcField(String name, String value) {
        fields.add(XMLElement.createElement(DC_NAMESPACE, name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
        fields.add(XMLElement.createElement(DC_NAMESPACE, name, value).withAttribute("xml:lang", lang));
    }

    public DCXMLDocument addDCElement(String elementName, JsonObject meta) {
    	return addDCElement(elementName, meta, elementName);
    }
    
    public DCXMLDocument addDCElement(String elementName, JsonObject meta, String tagName) {
    	if (meta.containsKey(elementName)) {
    		JsonArray titleArray = meta.getJsonArray(elementName);
            for(JsonObject title : titleArray.getValuesAs(JsonObject.class)) {
            	if (title.containsKey("lang")) {
            		this.addDcMultilingualField(tagName, 
                		title.getString("lang"), 
                		XMLDocument.removeHTMLTag(title.getString("value")));
            	} else {
            		this.addDcField(tagName, XMLDocument.removeHTMLTag(title.getString("value")));
            	}
            }
    	}
    	return this;
    }
    
}
