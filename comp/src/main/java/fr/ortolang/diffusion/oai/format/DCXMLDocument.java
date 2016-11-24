package fr.ortolang.diffusion.oai.format;

import javax.json.JsonArray;
import javax.json.JsonObject;

public abstract class DCXMLDocument extends XMLDocument {

    public void addDcField(String name, String value) {
        fields.add(XMLElement.createDcElement(name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
        fields.add(XMLElement.createDcElement(name, value).withAttribute("xml:lang", lang));
    }

    public DCXMLDocument addDCElement(String elementName, JsonObject meta) {
    	if (meta.containsKey(elementName)) {
    		JsonArray titleArray = meta.getJsonArray(elementName);
            for(JsonObject title : titleArray.getValuesAs(JsonObject.class)) {
            	if (title.containsKey("lang")) {
            		this.addDcMultilingualField(elementName, 
                		title.getString("lang"), 
                		XMLDocument.removeHTMLTag(title.getString("value")));
            	} else {
            		this.addDcField(elementName, XMLDocument.removeHTMLTag(title.getString("value")));
            	}
            }
    	}
    	return this;
    }
    
}
