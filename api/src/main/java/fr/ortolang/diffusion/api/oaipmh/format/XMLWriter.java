package fr.ortolang.diffusion.api.oaipmh.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class XMLWriter {

    protected String header;
    protected String footer;
    protected List<XMLElement> fields;

    public XMLWriter() {
        header = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">";
        footer = "</oai_dc:dc>";
        fields = new ArrayList<XMLElement>();
    }

    public void addDcField(String name, String value) {
        fields.add(XMLElement.createDcElement(name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
        fields.add(XMLElement.createDcElement(name, value).withAttribute("xml:lang", lang));
    }

    protected static void writeField(StringBuilder buffer, XMLElement elem) {

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

}
