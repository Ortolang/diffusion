package fr.ortolang.diffusion;

import java.util.Map;

public interface OrtolangObjectXmlImportHandler {
    
    void init(OrtolangObjectXmlImportHandler parentHandler);
    
    void startElement(String name, Map<String, String> attributes, OrtolangImportExportLogger logger);
    
    OrtolangObjectXmlImportHandler endElement(String name, OrtolangImportExportLogger logger);
    
    void content(String content, OrtolangImportExportLogger logger);

}
