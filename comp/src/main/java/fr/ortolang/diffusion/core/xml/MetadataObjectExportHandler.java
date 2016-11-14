package fr.ortolang.diffusion.core.xml;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class MetadataObjectExportHandler implements OrtolangObjectXmlExportHandler {
    
    private MetadataObject metadata; 
    
    public MetadataObjectExportHandler(MetadataObject metadata) {
        this.metadata = metadata;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", metadata.getId());
            attrs.put("name", metadata.getName());
            attrs.put("target", metadata.getTarget());
            attrs.put("format", metadata.getFormat());
            attrs.put("stream", metadata.getStream());
            attrs.put("content-type", metadata.getContentType());
            attrs.put("size", Long.toString(metadata.getSize()));
            XmlDumpHelper.outputEmptyElement("metadata", attrs, writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping collection", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(metadata.getTarget());
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(metadata.getStream());
        return deps;
    }

}
