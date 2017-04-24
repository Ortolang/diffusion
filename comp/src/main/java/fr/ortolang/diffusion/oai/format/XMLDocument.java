package fr.ortolang.diffusion.oai.format;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class XMLDocument {

    protected static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");
    
    private static HashMap<Pattern, String> patterns = new HashMap<Pattern, String>();
    
    static {
    	patterns.put(Pattern.compile("\\<.*?>", Pattern.DOTALL), "");
    	patterns.put(Pattern.compile("\\&nbsp;"), "");
    	patterns.put(Pattern.compile("\\&"), "");
    }
    
    protected String header;
    protected String footer;
    protected List<XMLElement> fields;

    public XMLDocument() {
    	fields = new ArrayList<XMLElement>();
    }

    public List<XMLElement> listFields(String name) throws IllegalStateException {
    	return fields.stream().filter(elm -> elm.getName().equals(name)).collect(Collectors.toList());
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
//    	return str.replaceAll("[\\n\\r]", "").replaceAll("\\<.*?>","").replaceAll("\\&nbsp;"," ").replaceAll("\\&","");
    	for(Map.Entry<Pattern, String> pattern : patterns.entrySet()) {
    		str = replace(str, pattern.getKey(), pattern.getValue());
    	}
    	return str;
    }
    
    protected static String replace(String str, Pattern pattern, String replacement) {
    	Matcher m = pattern.matcher(str);
    	return m.replaceAll(replacement);
    }
}
