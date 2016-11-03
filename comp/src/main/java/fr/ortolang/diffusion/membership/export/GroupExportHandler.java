package fr.ortolang.diffusion.membership.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;
import fr.ortolang.diffusion.membership.entity.Group;

public class GroupExportHandler implements OrtolangObjectExportHandler {
    
    private Group group; 
    
    public GroupExportHandler(Group group) {
        this.group = group;
   }
    
    @Override
    public void dumpObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", group.getId());
            attrs.put("name", group.getName());
            attrs.put("description", group.getDescription());
            XmlDumpHelper.startElement("membership", "group", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("group", "members", attrs, writer);
            for ( String member : group.getMembers() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("key", member);
                XmlDumpHelper.outputEmptyElement("group", "member", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            
            XmlDumpHelper.endElement(writer);
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping group", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        for ( String member : group.getMembers() ) {
            deps.add(member);
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
