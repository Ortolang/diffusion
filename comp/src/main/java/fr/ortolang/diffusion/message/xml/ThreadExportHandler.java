package fr.ortolang.diffusion.message.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class ThreadExportHandler implements OrtolangObjectXmlExportHandler {
    
    private Thread thread; 
    
    public ThreadExportHandler(Thread thread) {
        this.thread = thread;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", thread.getId());
            attrs.put("title", thread.getTitle());
            attrs.put("workspace", thread.getWorkspace());
            attrs.put("question", thread.getQuestion());
            attrs.put("answer", thread.getAnswer());
            attrs.put("last-activity", Long.toString(thread.getLastActivity().getTime()));
            XmlDumpHelper.startElement("message", "thread", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("thread", "observers", attrs, writer);
            for ( String observer : thread.getObservers() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("key", observer);
                XmlDumpHelper.outputEmptyElement("thread", "observer", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping thread", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(thread.getWorkspace());
        deps.add(thread.getQuestion());
        deps.add(thread.getAnswer());
        for ( String observer : thread.getObservers() ) {
            deps.add(observer);
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
