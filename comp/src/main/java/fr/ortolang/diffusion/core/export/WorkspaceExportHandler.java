package fr.ortolang.diffusion.core.export;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.MessageServiceException;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class WorkspaceExportHandler implements OrtolangObjectExportHandler {
    
    private Workspace workspace; 
    
    public WorkspaceExportHandler(Workspace workspace) {
        this.workspace = workspace;
   }
    
    @Override
    public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
        try {
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("id", workspace.getId());
            attrs.put("alias", workspace.getAlias());
            attrs.put("type", workspace.getType());
            attrs.put("name", workspace.getName());
            attrs.put("head", workspace.getHead());
            attrs.put("clock", Integer.toString(workspace.getClock()));
            attrs.put("changed", Boolean.toString(workspace.hasChanged()));
            attrs.put("readonly", Boolean.toString(workspace.isReadOnly()));
            attrs.put("members", workspace.getMembers());
            attrs.put("privileged", workspace.getPrivileged());
            attrs.put("archive", Boolean.toString(workspace.isArchive()));
            XmlDumpHelper.startElement("core", "workspace", attrs, writer);
            
            attrs = new XmlDumpAttributes();
            XmlDumpHelper.startElement("workspace", "snapshots", attrs, writer);
            for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", snapshot.getName());
                attrs.put("key", snapshot.getKey());
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
        } catch ( XMLStreamException e ) {
            throw new OrtolangException("error during dumping workspace", e);
        }
    }

    @Override
    public Set<String> getObjectDependencies() throws OrtolangException {
        Set<String> deps = new HashSet<String> ();
        deps.add(workspace.getHead());
        deps.add(workspace.getMembers());
        if ( workspace.getPrivileged() != null && workspace.getPrivileged().length() > 0 ) {
            deps.add(workspace.getPrivileged());
        }
        for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
            deps.add(snapshot.getKey());
        }
        MessageService mservice = (MessageService) OrtolangServiceLocator.findService(MessageService.SERVICE_NAME);
        try {
            deps.addAll(mservice.findThreadsForWorkspace(workspace.getKey()));
        } catch (MessageServiceException e) {
            throw new OrtolangException(e);
        }
        return deps;
    }

    @Override
    public Set<String> getObjectBinaryStreams() throws OrtolangException {
        return Collections.emptySet();
    }

}
