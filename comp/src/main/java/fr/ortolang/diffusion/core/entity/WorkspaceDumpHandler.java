package fr.ortolang.diffusion.core.entity;

import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.dump.XmlDumpAttributes;
import fr.ortolang.diffusion.dump.XmlDumpHelper;

public class WorkspaceDumpHandler {
    
    public static void dump(Workspace workspace, XMLStreamWriter writer, Set<String> deps, Set<String> streams) throws XMLStreamException {
        XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("id", workspace.getId());
        attrs.put("alias", workspace.getAlias());
        attrs.put("type", workspace.getType());
        attrs.put("name", workspace.getName());
        attrs.put("head", workspace.getHead());
        deps.add(workspace.getHead());
        attrs.put("clock", Integer.toString(workspace.getClock()));
        attrs.put("changed", Boolean.toString(workspace.hasChanged()));
        attrs.put("readonly", Boolean.toString(workspace.isReadOnly()));
        attrs.put("members", workspace.getMembers());
        deps.add(workspace.getMembers());
        attrs.put("privileged", workspace.getPrivileged());
        if ( workspace.getPrivileged() != null && workspace.getPrivileged().length() > 0 ) {
            deps.add(workspace.getPrivileged());
        }
        attrs.put("archive", Boolean.toString(workspace.isArchive()));
        XmlDumpHelper.startElement("core", "workspace", attrs, writer);
        
        attrs = new XmlDumpAttributes();
        XmlDumpHelper.startElement("workspace", "snapshots", attrs, writer);
        for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
            attrs = new XmlDumpAttributes();
            attrs.put("name", snapshot.getName());
            attrs.put("key", snapshot.getKey());
            deps.add(snapshot.getKey());
            XmlDumpHelper.outputEmptyElement("workspace", "snapshot", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        
        attrs = new XmlDumpAttributes();
        XmlDumpHelper.startElement("workspace", "tags", attrs, writer);
        for ( TagElement tag : workspace.getTags() ) {
            attrs = new XmlDumpAttributes();
            attrs.put("name", tag.getName());
            attrs.put("snapshot", tag.getSnapshot());
            XmlDumpHelper.outputEmptyElement("workspace", "tag", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        
        XmlDumpHelper.endElement(writer);
    }

}
