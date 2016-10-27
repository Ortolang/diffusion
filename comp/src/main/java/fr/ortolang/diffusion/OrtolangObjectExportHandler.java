package fr.ortolang.diffusion;

import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

public interface OrtolangObjectExportHandler {
    
    //TODO include an ExportImport Logger in order to handle indexation of imported keys in a specific order
    //This is particularly important for referential import.
    void dumpObject(XMLStreamWriter writer) throws OrtolangException;
    
    Set<String> getObjectDependencies() throws OrtolangException;
    
    Set<String> getObjectBinaryStreams() throws OrtolangException;

}
