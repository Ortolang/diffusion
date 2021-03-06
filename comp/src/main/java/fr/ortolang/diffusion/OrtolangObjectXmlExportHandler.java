package fr.ortolang.diffusion;

import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

public interface OrtolangObjectXmlExportHandler {
    
    void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException;
    
    Set<String> getObjectDependencies() throws OrtolangException;
    
    Set<String> getObjectBinaryStreams() throws OrtolangException;

}
