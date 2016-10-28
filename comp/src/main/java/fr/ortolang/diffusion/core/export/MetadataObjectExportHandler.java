package fr.ortolang.diffusion.core.export;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;

public class MetadataObjectExportHandler implements OrtolangObjectExportHandler {
    
    private MetadataObject metadata; 
    
    public MetadataObjectExportHandler(MetadataObject metadata) {
        this.metadata = metadata;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", metadata.getId());
            attrs.put("name", metadata.getName());
            attrs.put("target", metadata.getTarget());
            //TODO find a way to export format...
            attrs.put("format", metadata.getFormat());
            attrs.put("stream", metadata.getStream());
            attrs.put("content-type", metadata.getContentType());
            attrs.put("size", Long.toString(metadata.getSize()));
            XmlDumpHelper.outputEmptyElement("collection", "metadata", attrs, writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping collection", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(metadata.getTarget());
        //TODO parse metadata content in order to detect in binary content deps
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(metadata.getStream());
        return deps;
    }

}
