package fr.ortolang.diffusion.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.converter.IndentingXMLStreamWriter;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.security.Base64Encoder;

import com.sun.mail.util.BASE64DecoderStream;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangImportExportLogger.LogType;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProviderService;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.OrtolangObjectXmlImportHandler;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicy;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.entity.Handle;

@Local(ImportExportService.class)
@Stateless(name = ImportExportService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("system")
public class ImportExportServiceBean implements ImportExportService {

    private static final Logger LOGGER = Logger.getLogger(ImportExportServiceBean.class.getName());

    @EJB
    private RegistryService registry;
    @EJB
    private AuthorisationService authorization;
    @EJB
    private EventService event;
    @EJB
    private HandleStoreService handle;
    @EJB
    private BinaryStoreService binary;
    @Resource
    private SessionContext ctx;
    
    public ImportExportServiceBean() {
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void dump(Set<String> keys, OutputStream output, OrtolangImportExportLogger logger, boolean withdeps, boolean withbinary) throws ImportExportServiceException {
        LOGGER.log(Level.INFO, "Starting dump");
        try {
            Path dump = Files.createTempFile("ortolang-dump", ".xml");

            try (OutputStream os = Files.newOutputStream(dump)) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm:ss");

                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(os));

                XmlDumpAttributes attrs = new XmlDumpAttributes();
                attrs.put("date", sdf.format(new Date()));
                attrs.put("version", OrtolangConfig.getInstance().getVersion());
                XmlDumpHelper.startDocument(attrs, writer);

                Queue<String> queue = new LinkedList<String>();
                List<String> treated = new ArrayList<String>();

                Set<String> deps = new HashSet<String>();
                Set<String> streams = new HashSet<String>();
                for (String key : keys) {
                    exportKey(key, writer, logger, deps, streams);
                    treated.add(key);
                    populateQueue(queue, treated, deps);
                    if (withdeps) {
                        LOGGER.log(Level.FINE, "Dumping entry dependencies");
                        String current = null;
                        while ((current = queue.poll()) != null) {
                            LOGGER.log(Level.FINE, "Dumping current queue key : " + current);
                            deps.clear();
                            exportKey(current, writer, logger, deps, streams);
                            treated.add(current);
                            populateQueue(queue, treated, deps);
                        }
                    }
                }

                XmlDumpHelper.endDocument(writer);
                writer.flush();
                writer.close();

                try (GzipCompressorOutputStream gout = new GzipCompressorOutputStream(output); TarArchiveOutputStream out = new TarArchiveOutputStream(gout)) {
                    TarArchiveEntry entry = new TarArchiveEntry("ortolang-dump.xml");
                    entry.setModTime(System.currentTimeMillis());
                    entry.setSize(Files.size(dump));
                    try (InputStream isdump = Files.newInputStream(dump)) {
                        out.putArchiveEntry(entry);
                        IOUtils.copy(isdump, out);
                    } catch (IOException e) {
                        throw new ImportExportServiceException("unable to add dump to archive", e);
                    } finally {
                        try {
                            out.closeArchiveEntry();
                        } catch (IOException e) {
                            throw new ImportExportServiceException("unable to close archive entry for xml dump", e);
                        }
                    }

                    if (withbinary) {
                        for (String stream : streams) {
                            try (InputStream input = binary.get(stream)) {
                                TarArchiveEntry sentry = new TarArchiveEntry(stream);
                                sentry.setModTime(System.currentTimeMillis());
                                sentry.setSize(binary.size(stream));
                                try {
                                    out.putArchiveEntry(sentry);
                                    IOUtils.copy(input, out);
                                } catch (IOException e) {
                                    throw new ImportExportServiceException("unable to dump binary stream for hash: " + stream, e);
                                } finally {
                                    try {
                                        out.closeArchiveEntry();
                                    } catch (IOException e) {
                                        throw new ImportExportServiceException("unable to close archive entry for binary stream with hash: " + stream, e);
                                    }
                                }
                            } catch (DataNotFoundException | IOException | BinaryStoreServiceException e) {
                                throw new ImportExportServiceException(e);
                            }
                        }
                    }
                    out.flush();
                }
            }

        } catch (Exception e) {
            throw new ImportExportServiceException("Problem during dump", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void restore(InputStream input, OrtolangImportExportLogger logger) throws ImportExportServiceException {
        LOGGER.log(Level.INFO, "Restoring dump archive");
        try {
            Path dump = Files.createTempFile("ortolang-dump-", ".tmp");
            boolean dumpfound = false;

            try (GzipCompressorInputStream gin = new GzipCompressorInputStream(input); TarArchiveInputStream in = new TarArchiveInputStream(gin)) {
                ArchiveEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        try (InputStream is = new BoundedInputStream(in, entry.getSize())) {
                            if (entry.getName().equals("ortolang-dump.xml")) {
                                Files.copy(is, dump, StandardCopyOption.REPLACE_EXISTING);
                                dumpfound = true;
                            } else {
                                String filename = entry.getName();
                                if (filename.matches("[0-9a-f]{40}")) {
                                    String sha1 = binary.put(is);
                                    if (!sha1.equals(filename)) {
                                        logger.log(LogType.ERROR, "stream import error for : " + filename + ", sha1 found : " + sha1);
                                        try {
                                            binary.delete(sha1);
                                        } catch (DataNotFoundException e) {
                                        }
                                        throw new ImportExportServiceException("Some binary content could not be imported, aborting restoration");
                                    } else {
                                        logger.log(LogType.APPEND, "stream imported : " + filename);
                                    }
                                } else {
                                    logger.log(LogType.ERROR, "archive entry is not a sha1 base filename: " + filename);
                                    throw new ImportExportServiceException("Archive content error or unsupported structure, aborting restoration");
                                }
                            }
                        }
                    } else {
                        logger.log(LogType.ERROR, "archive content format error, directory not allowed : " + entry.getName());
                        throw new ImportExportServiceException("Archive content error or unsupported structure, aborting restoration");
                    }
                }
            }

            if (!dumpfound) {
                LOGGER.log(Level.WARNING, "no dmp file found in archive, nothing to import...");
                logger.log(LogType.ERROR, "no dump file found in archive, unable to import somehting");
            } else {
                LOGGER.log(Level.FINE, "starting dump restore...");
                logger.log(LogType.APPEND, "starting dump restore...");
                restoreDump(dump, logger);
                logger.log(LogType.APPEND, "restore complete.");
            }

        } catch (IOException | BinaryStoreServiceException | DataCollisionException e) {
            throw new ImportExportServiceException("Problem during restore", e);
        }

    }

    private void populateQueue(Queue<String> queue, List<String> treated, Set<String> deps) {
        LOGGER.log(Level.FINE, "Populating queue with new dependencies");
        for (String dep : deps) {
            if (!treated.contains(dep) && !queue.contains(dep)) {
                queue.add(dep);
                LOGGER.log(Level.INFO, "Adding new key in dump queue : " + dep);
            }
        }
        LOGGER.log(Level.FINE, "Queue size : " + queue.size());
    }

    private void exportKey(String key, XMLStreamWriter writer, OrtolangImportExportLogger logger, Set<String> deps, Set<String> streams) throws Exception {
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
        XmlDumpHelper.startElement("registry-entry", attrs, writer);

        // Properties
        LOGGER.log(Level.FINEST, "dumping entry properties");
        XmlDumpHelper.startElement("entry-properties", null, writer);
        for (String propertyName : entry.getProperties().stringPropertyNames()) {
            attrs = new XmlDumpAttributes();
            attrs.put("name", propertyName);
            attrs.put("value", entry.getProperties().getProperty(propertyName));
            XmlDumpHelper.outputEmptyElement("entry-property", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);

        // Security Policy
        LOGGER.log(Level.FINEST, "dumping authorisation policy");
        String owner = authorization.getPolicyOwner(key);
        attrs = new XmlDumpAttributes();
        attrs.put("key", key);
        attrs.put("owner", owner);
        XmlDumpHelper.startElement("security-policy", attrs, writer);
        for (Entry<String, List<String>> rule : authorization.getPolicyRules(key).entrySet()) {
            attrs = new XmlDumpAttributes();
            attrs.put("subject", rule.getKey());
            attrs.put("permissions", String.join(",", rule.getValue()));
            XmlDumpHelper.outputEmptyElement("security-rule", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);
        deps.add(owner);

        // Events
//        LOGGER.log(Level.FINEST, "dumping entry events");
//        @SuppressWarnings("unchecked")
//        List<OrtolangEvent> events = (List<OrtolangEvent>) event.systemListAllEventsForKey(key);
//        for (OrtolangEvent event : events) {
//            attrs = new XmlDumpAttributes();
//            attrs.put("type", event.getType());
//            attrs.put("date", event.getFormattedDate());
//            attrs.put("from-object", event.getFromObject());
//            attrs.put("object-type", event.getObjectType());
//            attrs.put("throwed-by", event.getThrowedBy());
//            XmlDumpHelper.startElement("entry-event", attrs, writer);
//            for (Entry<String, String> argument : event.getArguments().entrySet()) {
//                attrs = new XmlDumpAttributes();
//                attrs.put("name", argument.getKey());
//                attrs.put("value", argument.getValue());
//                XmlDumpHelper.outputEmptyElement("entry-event-arg", attrs, writer);
//            }
//            XmlDumpHelper.endElement(writer);
//            deps.add(event.getThrowedBy());
//        }
        
        // Handles
        LOGGER.log(Level.FINEST, "dumping entry handles");
        for (Handle hdl : handle.listHandlesValuesForKey(key)) {
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
            XmlDumpHelper.outputEmptyElement("entry-handle", attrs, writer);
        }
        
        deps.add(entry.getAuthor());
        if (entry.getParent() != null && entry.getParent().length() > 0) {
            deps.add(entry.getParent());
        }
        if (entry.getChildren() != null && entry.getChildren().length() > 0) {
            deps.add(entry.getChildren());
        }

        if (entry.getIdentifier() != null && entry.getIdentifier().length() > 0) {
            LOGGER.log(Level.FINEST, "dumping entry concrete object");

            OrtolangObjectIdentifier identifier = OrtolangObjectIdentifier.deserialize(entry.getIdentifier());
            OrtolangObjectProviderService service = OrtolangServiceLocator.findObjectProviderService(identifier.getService());

            attrs = new XmlDumpAttributes();
            attrs.put("service", identifier.getService());
            attrs.put("type", identifier.getType());
            XmlDumpHelper.startElement("ortolang-object", attrs, writer);

            try {
                OrtolangObjectXmlExportHandler handler = service.getObjectXmlExportHandler(key);
                handler.exportObject(writer, logger);
                deps.addAll(handler.getObjectDependencies());
                streams.addAll(handler.getObjectBinaryStreams());
            } catch (OrtolangException e) {
                logger.log(LogType.ERROR, key + " " + e.getMessage());
            }

            XmlDumpHelper.endElement(writer);
        }

        XmlDumpHelper.endElement(writer);
    }

    private void restoreDump(Path dump, OrtolangImportExportLogger logger) throws ImportExportServiceException {
        try (InputStream input = Files.newInputStream(dump)) {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlif.createXMLStreamReader(input);
            OrtolangObjectXmlImportHandler handler = null;
            RegistryEntry entry = null;
            AuthorisationPolicy policy = null;
            List<Handle> handles = null;
            while (reader.hasNext()) {
                reader.next();
                if (reader.isStartElement()) {
                    Map<String, String> attributes = new HashMap<String, String>();
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                        LOGGER.log(Level.FINE, "Found element attribute: " + reader.getAttributeLocalName(i) + " = " + reader.getAttributeValue(i));
                    }
                    
                    if ( handler != null ) {
                        handler.startElement(reader.getName().getLocalPart(), attributes, logger);
                    }
                    
                    if (reader.getName().getLocalPart().equals("ortolang-dump")) {
                        LOGGER.log(Level.FINE, "Start of dump");
                        if (!attributes.get("version").equals(OrtolangConfig.getInstance().getVersion())) {
                            ctx.setRollbackOnly();
                            throw new ImportExportServiceException("Version mismatch. Try to restore a dump that is not compatible with this version");
                        }
                    }
                    
                    if (reader.getName().getLocalPart().equals("registry-entry")) {
                        LOGGER.log(Level.FINE, "New Entry");
                        entry = new RegistryEntry();
                        handles = new ArrayList<Handle> ();
                        policy = new AuthorisationPolicy();
                        
                        entry.setKey(attributes.get("key"));
                        entry.setIdentifier(attributes.get("identifier"));
                        entry.setAuthor(attributes.get("author"));
                        entry.setCreationDate(Long.parseLong(attributes.get("creation-timestamp")));
                        entry.setLastModificationDate(Long.parseLong(attributes.get("modification-timestamp")));
                        entry.setAuthor(attributes.get("author"));
                        entry.setPublicationStatus(attributes.get("publication-status"));
                        entry.setHidden(Boolean.parseBoolean(attributes.get("hidden")));
                        entry.setDeleted(Boolean.parseBoolean(attributes.get("deleted")));
                        if ( attributes.get("lock").length() > 0 ) {
                            entry.setLock(attributes.get("lock"));
                        }
                        if ( attributes.get("parent").length() > 0 ) {
                            entry.setParent(attributes.get("parent"));
                        }
                        if ( attributes.get("children").length() > 0 ) {
                            entry.setChildren(attributes.get("children"));
                        }
                    }
                    
                    if (reader.getName().getLocalPart().equals("entry-property")) {
                        entry.setProperty(attributes.get("name"), attributes.get("value"));
                    }
                    
                    if (reader.getName().getLocalPart().equals("security-policy")) {
                        policy.setId(attributes.get("key"));
                        policy.setOwner(attributes.get("owner"));
                    }
                    
                    if (reader.getName().getLocalPart().equals("security-rule")) {
                        policy.setPermissions(attributes.get("subject"), Arrays.asList(attributes.get("permissions").split(",")) );
                    }
                    
                    if (reader.getName().getLocalPart().equals("entry-handle")) {
                        Handle handle = new Handle();
                        handle.setKey(attributes.get("key"));
                        handle.setHandle(attributes.get("handle").getBytes());
                        handle.setType(attributes.get("type").getBytes());
                        handle.setTtl(Integer.parseInt(attributes.get("ttl")));
                        handle.setTtlType(Short.parseShort(attributes.get("ttl-type")));
                        handle.setTimestamp(Integer.parseInt(attributes.get("timestamp")));
                        handle.setRefs(attributes.get("refs"));
                        handle.setIndex(Integer.parseInt(attributes.get("index")));
                        String permissions = attributes.get("permissions");
                        handle.setAdminRead((permissions.charAt(0) == '1')?true:false);
                        handle.setAdminWrite((permissions.charAt(1) == '1')?true:false);
                        handle.setPubRead((permissions.charAt(2) == '1')?true:false);
                        handle.setPubWrite((permissions.charAt(3) == '1')?true:false);
                        handle.setData(BASE64DecoderStream.decode(attributes.get("data").getBytes()));
                        handles.add(handle);
                    }
                    
                    if (reader.getName().getLocalPart().equals("ortolang-object")) {
                        try {
                            OrtolangObjectProviderService provider = OrtolangServiceLocator.findObjectProviderService(attributes.get("service"));
                            handler = provider.getObjectXmlImportHandler(attributes.get("type"));
                        } catch ( OrtolangException e ) {
                            ctx.setRollbackOnly();
                            throw new ImportExportServiceException("Restore failed : unable to find a service with name: " + attributes.get("service"));
                        }
                    }
                    
                }

                if (reader.isEndElement()) {
                    if ( handler != null ) {
                        handler = handler.endElement(reader.getName().getLocalPart(), logger);
                    }
                    
                    if (reader.getName().getLocalPart().equals("ortolang-dump")) {
                        LOGGER.log(Level.FINE, "End of dump");
                    }
                    
                    if (reader.getName().getLocalPart().equals("registry-entry")) {
                        if ( entry != null ) {
                            LOGGER.log(Level.FINE, "restoring entry for key: " + entry.getKey());
                            //TODO Careful, restoring profiles can cause troubles, maybe takes a special strategy for importing workspaces
                            if ( policy != null ) {
                                
                            }
                            if ( policy != null ) {
                                
                            }
                        }
                        
                        entry = null;
                    }
                    
                    
                }

                if (reader.isCharacters()) {
                    if ( handler != null ) {
                        handler.content(reader.getText(), logger);
                    }
                }
            }

        } catch (IOException | XMLStreamException e) {
            throw new ImportExportServiceException(e);
        }
    }

    @Override
    public String getServiceName() {
        return ImportExportService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

}

