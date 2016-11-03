package fr.ortolang.diffusion;

import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

public interface OrtolangObjectExportHandler {
    
    void dumpObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException;
    
    Set<String> getObjectDependencies() throws OrtolangException;
    
    Set<String> getObjectBinaryStreams() throws OrtolangException;

}
