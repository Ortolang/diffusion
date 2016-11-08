package fr.ortolang.diffusion.core.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class CollectionExportHandler implements OrtolangObjectExportHandler {
    
    private Collection collection; 
    
    public CollectionExportHandler(Collection collection) {
        this.collection = collection;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", collection.getId());
            attrs.put("name", collection.getName());
            attrs.put("clock", Integer.toString(collection.getClock()));
            XmlDumpHelper.startElement("core", "collection", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("collection", "elements", attrs, writer);
            for ( CollectionElement element : collection.getElements() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", element.getName());
                attrs.put("key", element.getKey());
                attrs.put("type", element.getType());
                attrs.put("mime-type", element.getMimeType());
                attrs.put("size", Long.toString(element.getSize()));
                attrs.put("modification-timestamp", Long.toString(element.getModification()));
                XmlDumpHelper.outputEmptyElement("collection", "element", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("collection", "metadatas", attrs, writer);
            for ( MetadataElement element : collection.getMetadatas() ) {
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
        for ( CollectionElement element : collection.getElements() ) {
            deps.add(element.getKey());
        }
        for ( MetadataElement element : collection.getMetadatas() ) {
            deps.add(element.getKey());
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
