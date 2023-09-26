package fr.ortolang.diffusion.api.oai.metadata;

import java.util.List;

public class GenericOaiXmlDocument implements OaiXmlDocument {

    private List<OaiXmlValue> values;

    @Override
    public List<OaiXmlValue> getValues() {
        return values;
    }
    
    @Override
    public void setValues(List<OaiXmlValue> values) {
        this.values = values;
    }

}
