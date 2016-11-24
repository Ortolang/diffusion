package fr.ortolang.diffusion.oai.format;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class XMLDocument {

    protected static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");
    
    protected String header;
    protected String footer;
    protected List<XMLElement> fields;

    public XMLDocument() {
    	fields = new ArrayList<XMLElement>();
    }

    protected void writeField(StringBuilder buffer, XMLElement elem) {

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

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(header);

        for(XMLElement elem : fields) {
            writeField(buffer, elem);
        }

        buffer.append(footer);

        return buffer.toString();
    }

    public static String removeHTMLTag(String str) {
        return str.replaceAll("\\<.*?>","").replaceAll("\\&nbsp;"," ").replaceAll("\\&","");
    }
}
