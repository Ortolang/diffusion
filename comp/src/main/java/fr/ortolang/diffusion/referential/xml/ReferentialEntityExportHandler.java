package fr.ortolang.diffusion.referential.xml;

import java.util.Collections;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class ReferentialEntityExportHandler implements OrtolangObjectXmlExportHandler {
    
    private ReferentialEntity entity; 
    
    public ReferentialEntityExportHandler(ReferentialEntity entity) {
        this.entity = entity;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", entity.getId());
            attrs.put("type", entity.getType().name());
            attrs.put("boost", Long.toString(entity.getBoost()));
            XmlDumpHelper.startElement("referential-entity", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.outputElementWithData("referential-entity-content", attrs, entity.getContent(), writer);

            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping referential entity", e);
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
