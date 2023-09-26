package fr.ortolang.diffusion.api.oai.metadata;

import java.util.List;

public interface OaiXmlDocument {
    List<OaiXmlValue> getValues();
    void setValues(List<OaiXmlValue> values);
}
