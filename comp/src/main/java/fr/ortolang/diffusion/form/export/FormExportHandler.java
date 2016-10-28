package fr.ortolang.diffusion.form.export;

import java.util.Collections;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;
import fr.ortolang.diffusion.form.entity.Form;

public class FormExportHandler implements OrtolangObjectExportHandler {
    
    private Form form; 
    
    public FormExportHandler(Form form) {
        this.form = form;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", form.getId());
            attrs.put("name", form.getName());
            attrs.put("definition", form.getDefinition());
            XmlDumpHelper.outputEmptyElement("form", "form", attrs, writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping form", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
