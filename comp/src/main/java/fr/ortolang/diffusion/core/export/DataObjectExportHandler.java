package fr.ortolang.diffusion.core.export;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;

public class DataObjectExportHandler implements OrtolangObjectExportHandler {
    
    private DataObject object; 
    
    public DataObjectExportHandler(DataObject object) {
        this.object = object;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", object.getId());
            attrs.put("name", object.getName());
            attrs.put("mime-type", object.getMimeType());
            attrs.put("size", Long.toString(object.getSize()));
            attrs.put("stream", object.getStream());
            attrs.put("clock", Integer.toString(object.getClock()));
            XmlDumpHelper.startElement("core", "object", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("object", "metadatas", attrs, writer);
            for ( MetadataElement element : object.getMetadatas() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", element.getName());
                attrs.put("key", element.getKey());
                XmlDumpHelper.outputEmptyElement("collection", "metadata", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping collection", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        for ( MetadataElement element : object.getMetadatas() ) {
            deps.add(element.getKey());
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(object.getStream());
        return deps;
    }

}
