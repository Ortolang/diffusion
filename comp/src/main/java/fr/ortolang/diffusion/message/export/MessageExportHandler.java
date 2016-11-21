package fr.ortolang.diffusion.message.export;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.MessageAttachment;

public class MessageExportHandler implements OrtolangObjectExportHandler {
    
    private Message message; 
    
    public MessageExportHandler(Message message) {
        this.message = message;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", message.getId());
            attrs.put("thread", message.getThread());
            if ( message.getParent() != null ) {
                attrs.put("parent", message.getParent());
            } else {
                attrs.put("parent", "");
            }
            attrs.put("date", Long.toString(message.getDate().getTime()));
            attrs.put("edit", Long.toString(message.getEdit().getTime()));
            XmlDumpHelper.startElement("message", "message", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.outputElementWithContent("message", "content", attrs, message.getBody(), writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("message", "attachements", attrs, writer);
            for ( MessageAttachment attachment : message.getAttachments() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", attachment.getName());
                attrs.put("type", attachment.getType());
                attrs.put("hash", attachment.getHash());
                attrs.put("size", Long.toString(attachment.getSize()));
                XmlDumpHelper.outputEmptyElement("message", "attachment", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping message", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        if ( message.getThread() != null ) {
            deps.add(message.getThread());
        }
        if ( message.getParent() != null ) {
            deps.add(message.getParent());
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        Set<String> streams = new HashSet<String> ();
        for ( MessageAttachment attachment : message.getAttachments() ) {
            streams.add(attachment.getHash());
        }
        return streams;
    }

}
