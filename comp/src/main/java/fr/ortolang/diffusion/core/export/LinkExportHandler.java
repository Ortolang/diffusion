package fr.ortolang.diffusion.core.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class LinkExportHandler implements OrtolangObjectExportHandler {
    
    private Link link; 
    
    public LinkExportHandler(Link link) {
        this.link = link;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", link.getId());
            attrs.put("name", link.getName());
            attrs.put("target", link.getTarget());
            attrs.put("clock", Integer.toString(link.getClock()));
            XmlDumpHelper.startElement("core", "link", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("link", "metadatas", attrs, writer);
            for ( MetadataElement element : link.getMetadatas() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", element.getName());
                attrs.put("key", element.getKey());
                XmlDumpHelper.outputEmptyElement("link", "metadata", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping link", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(link.getTarget());
        for ( MetadataElement element : link.getMetadatas() ) {
            deps.add(element.getKey());
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
