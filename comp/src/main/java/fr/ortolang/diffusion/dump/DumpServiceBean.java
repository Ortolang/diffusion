package fr.ortolang.diffusion.dump;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.converter.IndentingXMLStreamWriter;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.security.Base64Encoder;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProviderService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.entity.Handle;

@Local(DumpService.class)
@Stateless(name = DumpService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("system")
public class DumpServiceBean implements DumpService {
    
    private static final Logger LOGGER = Logger.getLogger(DumpServiceBean.class.getName());
    
    @EJB
    private RegistryService registry;
    @EJB
    private AuthorisationService authorization;
    @EJB
    private EventService event;
    @EJB
    private HandleStoreService handle;

    @Override
    public Set<String> dump(String key, OutputStream output, boolean single) throws DumpServiceException {
        LOGGER.log(Level.INFO, "Starting dump of key : " + key);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm:ss");
            
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(output));
            
            XmlDumpAttributes attrs = new XmlDumpAttributes();
            attrs.put("date", sdf.format(new Date()));
            attrs.put("version", OrtolangConfig.getInstance().getVersion());
            XmlDumpHelper.startDocument("ortolang", "ortolang-dump", attrs, writer);
            
            Queue<String> queue = new LinkedList<String>();
            List<String> treated = new ArrayList<String>();   
            
            Set<String> deps = new HashSet<String>();
            Set<String> streams = new HashSet<String>();
            dumpEntry(key, writer, deps, streams);
            treated.add(key);
            populateQueue(queue, treated, deps);
            
            if ( !single ) {
                LOGGER.log(Level.FINE, "Dumping entry dependencies");
                String current = null;
                while ( (current = queue.poll()) != null ) {
                    LOGGER.log(Level.INFO, "Current queue key : " + current);
                    deps.clear();
                    dumpEntry(current, writer, deps, streams);
                    treated.add(current);
                    populateQueue(queue, treated, deps);
                }
            }
            
            XmlDumpHelper.endElement(writer);
            XmlDumpHelper.endDocument(writer);
            writer.flush();
            writer.close();
            
            return streams;
        } catch (Exception e) {
            throw new DumpServiceException("Problem during dump of key: " + key, e);
        }
    }
    
    private void populateQueue(Queue<String> queue, List<String> treated, Set<String> deps) {
        LOGGER.log(Level.FINE, "Populating queue with new dependencies");
        for ( String dep : deps ) {
            if ( !treated.contains(dep) && !queue.contains(dep) ) {
                queue.add(dep);
                LOGGER.log(Level.INFO, "Adding new key in dump queue : " + dep);
            }
        }
        LOGGER.log(Level.FINE, "Queue size : " + queue.size());
    }
    
    private void dumpEntry(String key, XMLStreamWriter writer, Set<String> deps, Set<String> streams) throws Exception {
        LOGGER.log(Level.FINE, "dumping entry : " + key);
        RegistryEntry entry = registry.systemReadEntry(key);
        XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("key", entry.getKey());
        attrs.put("identifier", entry.getIdentifier());
        attrs.put("parent", entry.getParent());
        attrs.put("children", entry.getChildren());
        attrs.put("author", entry.getAuthor());
        attrs.put("creation-timestamp", Long.toString(entry.getCreationDate()));
        attrs.put("modification-timestamp", Long.toString(entry.getLastModificationDate()));
        attrs.put("lock", entry.getLock());
        attrs.put("hidden", Boolean.toString(entry.isHidden()));
        attrs.put("deleted", Boolean.toString(entry.isDeleted()));
        attrs.put("publication-status", entry.getPublicationStatus());
        XmlDumpHelper.startElement("registry", "entry", attrs, writer); 
        
        //Properties
        LOGGER.log(Level.FINEST, "dumping entry properties");
        XmlDumpHelper.startElement("entry", "properties", null, writer);
        for ( String propertyName : entry.getProperties().stringPropertyNames() ) {
            attrs = new XmlDumpAttributes();
            attrs.put("name", propertyName);
            attrs.put("value", entry.getProperties().getProperty(propertyName));
            XmlDumpHelper.outputEmptyElement("entry", "property", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        
        //Security Policy
        LOGGER.log(Level.FINEST, "dumping entry security policy");
        String owner = authorization.getPolicyOwner(key);
        attrs = new XmlDumpAttributes();
        attrs.put("owner", owner);
        XmlDumpHelper.startElement("entry", "security-policy", attrs, writer);
        for ( Entry<String, List<String>> rule : authorization.getPolicyRules(key).entrySet() ) {
            attrs = new XmlDumpAttributes();
            attrs.put("subject", rule.getKey());
            attrs.put("permissions", String.join(",", rule.getValue()));
            XmlDumpHelper.outputEmptyElement("security-policy", "rule", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        deps.add(owner);
        
        //Events
        LOGGER.log(Level.FINEST, "dumping entry events");
        @SuppressWarnings("unchecked")
        List<OrtolangEvent> events = (List<OrtolangEvent>) event.systemListAllEventsForKey(key);
        XmlDumpHelper.startElement("entry", "events", null, writer);
        for ( OrtolangEvent event : events ) {
            attrs = new XmlDumpAttributes();
            attrs.put("type", event.getType());
            attrs.put("date", event.getFormattedDate());
            attrs.put("from-object", event.getFromObject());
            attrs.put("object-type", event.getObjectType());
            attrs.put("throwed-by", event.getThrowedBy());
            XmlDumpHelper.startElement("entry", "event", attrs, writer);
            for ( Entry<String, String> argument : event.getArguments().entrySet() ) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", argument.getKey());
                attrs.put("value", argument.getValue());
                XmlDumpHelper.outputEmptyElement("event", "argument", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            deps.add(event.getThrowedBy());
        }
        XmlDumpHelper.endElement(writer);
        
        //Handles
        LOGGER.log(Level.FINEST, "dumping entry handles");
        XmlDumpHelper.startElement("entry", "handles", null, writer);
        for ( Handle hdl : handle.listHandlesValuesForKey(key) ) {
            attrs = new XmlDumpAttributes();
            attrs.put("key", hdl.getKey());
            attrs.put("handle", hdl.getHandleString());
            attrs.put("type", hdl.getTypeString());
            attrs.put("ttl", Integer.toString(hdl.getTtl()));
            attrs.put("ttl-type", Short.toString(hdl.getTtlType()));
            attrs.put("timestamp", Integer.toString(hdl.getTimestamp()));
            attrs.put("refs", hdl.getRefs());
            attrs.put("index", Integer.toString(hdl.getIndex()));
            attrs.put("permissions", hdl.getPermissionsString());
            attrs.put("data", Base64Encoder.encode(hdl.getData()));
            //TODO Try to avoid exporting HS_ADMIN handles
            XmlDumpHelper.outputEmptyElement("entry", "handle", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        
        deps.add(entry.getAuthor());
        if ( entry.getParent() != null && entry.getParent().length() > 0 ) {
            deps.add(entry.getParent());
        }
        if ( entry.getChildren() != null && entry.getChildren().length() > 0 ) {
            deps.add(entry.getChildren());
        }
        
        if ( entry.getIdentifier() != null && entry.getIdentifier().length() > 0 ) {
            LOGGER.log(Level.FINEST, "dumping entry concrete object");
            
            OrtolangObjectIdentifier identifier = OrtolangObjectIdentifier.deserialize(entry.getIdentifier());
            OrtolangObjectProviderService service = OrtolangServiceLocator.findObjectProviderService(identifier.getService());
            try {
                OrtolangObjectExportHandler handler = service.getObjectExportHandler(key);
                handler.dumpObject(writer);
                deps.addAll(handler.getObjectDependencies());
                streams.addAll(handler.getObjectBinaryStreams());
            } catch ( OrtolangException e ) {
                //TODO log this lake of export to ImportExportLogger
            }
        }
        
        XmlDumpHelper.endElement(writer);
    }

    @Override
    public void restore(InputStream input) throws DumpServiceException {
        // TODO Auto-generated method stub
        throw new DumpServiceException("Problem during import: NOT IMPLEMENTED");
    }

    @Override
    public String getServiceName() {
        return DumpService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    
}
