package fr.ortolang.diffusion.api.oai.metadata.cmdi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import fr.ortolang.diffusion.api.oai.metadata.OaiXmlValue;

public class CmdiParser {

    private static final Logger LOGGER = Logger.getLogger(CmdiParser.class.getName());

    private CmdiDocument doc;

    public CmdiParser() {
        this.doc = new CmdiDocument();
    }

    public CmdiDocument getDoc() {
        return doc;
    }

    public static CmdiParser newInstance() {
        return new CmdiParser();
    }

    public CmdiParser parse(String xml) throws DocumentException {
        Document document = DocumentHelper.parseText(xml);
        Node nodeMdSelfLink = document.selectSingleNode(CmdiDocumentConstants.MDSELFLINK_PATH.key());
        LOGGER.log(Level.FINE, "MdSelfLink {0}", nodeMdSelfLink);

        List<OaiXmlValue> cmdiValues = new ArrayList<>();
        Element nodeComponents = (Element) document.selectSingleNode(CmdiDocumentConstants.COMPONENTS_PATH.key());
        walkThrough(nodeComponents, cmdiValues);

        LOGGER.log(Level.FINE, "Nb CMDI components {0}", cmdiValues.size());
        
        this.doc.setMdSelfLink(nodeMdSelfLink.getStringValue());
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
