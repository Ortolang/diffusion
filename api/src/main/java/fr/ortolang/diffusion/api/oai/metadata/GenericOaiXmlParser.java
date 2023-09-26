package fr.ortolang.diffusion.api.oai.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

public class GenericOaiXmlParser {
    
    private static final Logger LOGGER = Logger.getLogger(GenericOaiXmlParser.class.getName());

    private GenericOaiXmlDocument doc;

    public GenericOaiXmlParser() {
        this.doc = new GenericOaiXmlDocument();
    }

    public GenericOaiXmlDocument getDoc() {
        return doc;
    }

    public static GenericOaiXmlParser newInstance() {
        return new GenericOaiXmlParser();
    }

    public GenericOaiXmlParser parse(String xml) throws DocumentException {
        Document document = DocumentHelper.parseText(xml);

        List<OaiXmlValue> cmdiValues = new ArrayList<>();
        Element nodeComponents = document.getRootElement();
        walkThrough(nodeComponents, cmdiValues);
        LOGGER.log(Level.FINE, "Nb XML elements {0}", cmdiValues.size());

        this.doc.setValues(cmdiValues);
        return this;
    }

    private void walkThrough(Element node, List<OaiXmlValue> cmdiValues) {
        if ( node.isTextOnly() ) {
            cmdiValues.add(new OaiXmlValue(node.getName(), node.getStringValue()));
            LOGGER.log(Level.FINE, "Value {0}", node.getStringValue());
        }

        // iterate through child elements
        for (Element child : node.elements()) {
            LOGGER.log(Level.FINE, "element {0}", child.getName());
            LOGGER.log(Level.FINE, "Type {0}", child.getNodeType());

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                walkThrough(child, cmdiValues);
            }
        }
    }
}
