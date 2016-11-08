package fr.ortolang.diffusion.runtime.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class ProcessExportHandler implements OrtolangObjectXmlExportHandler {
    
    private Process process; 
    
    public ProcessExportHandler(Process process) {
        this.process = process;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", process.getId());
            attrs.put("name", process.getName());
            attrs.put("type", process.getType());
            attrs.put("initier", process.getInitier());
            attrs.put("activity", process.getActivity());
            attrs.put("workspace", process.getWorkspace());
            attrs.put("start", Long.toString(process.getStart()));
            attrs.put("stop", Long.toString(process.getStop()));
            attrs.put("status", process.getStatus());
            attrs.put("progress", Integer.toString(process.getProgress()));
            attrs.put("state", process.getState().name());
            XmlDumpHelper.startElement("runtime", "process", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.outputElementWithData("process", "log", attrs, process.getLog(), writer);

            attrs = new XmlDumpAttributes();
            XmlDumpHelper.outputElementWithData("process", "explanation", attrs, process.getExplanation(), writer);

            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping process", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(process.getInitier());
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
