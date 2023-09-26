package fr.ortolang.diffusion.api.oai.metadata.cmdi;

import java.util.List;

import fr.ortolang.diffusion.api.oai.metadata.OaiXmlDocument;
import fr.ortolang.diffusion.api.oai.metadata.OaiXmlValue;

public class CmdiDocument implements OaiXmlDocument {

    private String mdSelfLink;
    private List<OaiXmlValue> values;

    public List<OaiXmlValue> getValues() {
        return values;
    }

    public void setValues(List<OaiXmlValue> values) {
        this.values = values;
    }

    public String getMdSelfLink() {
        return mdSelfLink;
    }

    public void setMdSelfLink(String mdSelfLink) {
        this.mdSelfLink = mdSelfLink;
    }

    public CmdiDocument() {
        // Uses getter and setters
    }
}
