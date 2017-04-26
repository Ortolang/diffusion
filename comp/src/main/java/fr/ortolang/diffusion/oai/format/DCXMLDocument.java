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
    		JsonArray elmArray = meta.getJsonArray(elementName);
            for(JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
            	if (elm.containsKey("lang") && elm.containsKey("value")) {
            		this.addDcMultilingualField(tagName, 
                		elm.getString("lang"), 
                		XMLDocument.removeHTMLTag(elm.getString("value")));
            	} else {
            		if (elm.containsKey("value")) {
	            		this.addDcField(tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
	            	}
            	}
            }
    	}
    	return this;
    }
    
}
