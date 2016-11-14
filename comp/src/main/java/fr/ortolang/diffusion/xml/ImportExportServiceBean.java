package fr.ortolang.diffusion.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangImportExportLogger.LogType;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProviderService;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
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

    @Override
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
                    dumpEntry(key, writer, logger, deps, streams);
                    treated.add(key);
                    populateQueue(queue, treated, deps);
                    if (withdeps) {
                        LOGGER.log(Level.FINE, "Dumping entry dependencies");
                        String current = null;
                        while ((current = queue.poll()) != null) {
                            LOGGER.log(Level.FINE, "Dumping current queue key : " + current);
                            deps.clear();
                            dumpEntry(current, writer, logger, deps, streams);
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

    private void dumpEntry(String key, XMLStreamWriter writer, OrtolangImportExportLogger logger, Set<String> deps, Set<String> streams) throws Exception {
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
        XmlDumpHelper.startElement("properties", null, writer);
        for (String propertyName : entry.getProperties().stringPropertyNames()) {
            attrs = new XmlDumpAttributes();
            attrs.put("name", propertyName);
            attrs.put("value", entry.getProperties().getProperty(propertyName));
            XmlDumpHelper.outputEmptyElement("property", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);

        // Security Policy
        LOGGER.log(Level.FINEST, "dumping entry security policy");
        String owner = authorization.getPolicyOwner(key);
        attrs = new XmlDumpAttributes();
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
        LOGGER.log(Level.FINEST, "dumping entry events");
        @SuppressWarnings("unchecked")
        List<OrtolangEvent> events = (List<OrtolangEvent>) event.systemListAllEventsForKey(key);
        XmlDumpHelper.startElement("events", null, writer);
        for (OrtolangEvent event : events) {
            attrs = new XmlDumpAttributes();
            attrs.put("type", event.getType());
            attrs.put("date", event.getFormattedDate());
            attrs.put("from-object", event.getFromObject());
            attrs.put("object-type", event.getObjectType());
            attrs.put("throwed-by", event.getThrowedBy());
            XmlDumpHelper.startElement("event", attrs, writer);
            for (Entry<String, String> argument : event.getArguments().entrySet()) {
                attrs = new XmlDumpAttributes();
                attrs.put("name", argument.getKey());
                attrs.put("value", argument.getValue());
                XmlDumpHelper.outputEmptyElement("event-arg", attrs, writer);
            }
            XmlDumpHelper.endElement(writer);
            deps.add(event.getThrowedBy());
        }
        XmlDumpHelper.endElement(writer);

        // Handles
        LOGGER.log(Level.FINEST, "dumping entry handles");
        XmlDumpHelper.startElement("handles", null, writer);
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
            XmlDumpHelper.outputEmptyElement("handle", attrs, writer);
        }
        XmlDumpHelper.endElement(writer);

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

    @Override
    public void restore(InputStream input, OrtolangImportExportLogger logger) throws ImportExportServiceException {
        LOGGER.log(Level.INFO, "Restoring dump archive");
        try {
            Path dump = Files.createTempFile("ortolang-dump-", ".tmp");
            boolean dumpfound = false;

            try (GzipCompressorInputStream gin = new GzipCompressorInputStream(input); TarArchiveInputStream in = new TarArchiveInputStream(gin)) {
                // TODO if sha1, inport file directly in store, if dump, store it somewhere until sha one import is finished
                ArchiveEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if ( !entry.isDirectory() ) {
                        InputStream is = new BoundedInputStream(in, entry.getSize());
                        if (entry.getName().equals("ortolang-dump.xml")) {
                            Files.copy(is, dump, StandardCopyOption.REPLACE_EXISTING);
                            dumpfound = true;
                        } else {
                            String filename = entry.getName();
                            //TODO check that filename matches a sha1..
                            String sha1 = binary.put(is);
                            if (!sha1.equals(filename)) {
                                logger.log(LogType.ERROR, "stream import error for : " + filename + ", sha1 found : " + sha1);
                            } else {
                                logger.log(LogType.APPEND, "stream imported : " + filename);
                            }
                        }
                        is.close();
                    } else {
                        LOGGER.log(Level.WARNING, "archive content format error, directory not allowed : " + entry.getName());
                        logger.log(LogType.ERROR, "archive content format error, directory not allowed : " + entry.getName());
                    }
                }
            }

            if (!dumpfound) {
                LOGGER.log(Level.WARNING, "no dmp file found in archive, nothing to import...");
                logger.log(LogType.ERROR, "no dump file found in archive, unable to import somehting");
            } else {
                LOGGER.log(Level.FINE, "starting dump restore...");
                logger.log(LogType.APPEND, "starting dump restore...");
                restoreFile(dump, logger);
                logger.log(LogType.APPEND, "restore complete.");
            }

        } catch (Exception e) {
            throw new ImportExportServiceException("Problem during restore", e);
        }

    }

    private void restoreFile(Path dump, OrtolangImportExportLogger logger) throws ImportExportServiceException {
        try (InputStream input = Files.newInputStream(dump)) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                reader.nextTag();
                printEvent(reader);
            }

            reader.close();
        } catch (XMLStreamException | IOException e) {
            throw new ImportExportServiceException(e);
        }

    }

    private static void printEvent(XMLStreamReader xmlr) {

        System.out.print("EVENT:[" + xmlr.getLocation().getLineNumber() + "][" + xmlr.getLocation().getColumnNumber() + "] ");

        System.out.print(" [");

        switch (xmlr.getEventType()) {

        case XMLStreamConstants.START_ELEMENT:
            System.out.print("<");
            printName(xmlr);
            printNamespaces(xmlr);
            printAttributes(xmlr);
            System.out.print(">");
            break;

        case XMLStreamConstants.END_ELEMENT:
            System.out.print("</");
            printName(xmlr);
            System.out.print(">");
            break;

        case XMLStreamConstants.SPACE:

        case XMLStreamConstants.CHARACTERS:
            int start = xmlr.getTextStart();
            int length = xmlr.getTextLength();
            System.out.print(new String(xmlr.getTextCharacters(), start, length));
            break;

        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            System.out.print("<?");
            if (xmlr.hasText())
                System.out.print(xmlr.getText());
            System.out.print("?>");
            break;

        case XMLStreamConstants.CDATA:
            System.out.print("<![CDATA[");
            start = xmlr.getTextStart();
            length = xmlr.getTextLength();
            System.out.print(new String(xmlr.getTextCharacters(), start, length));
            System.out.print("]]>");
            break;

        case XMLStreamConstants.COMMENT:
            System.out.print("<!--");
            if (xmlr.hasText())
                System.out.print(xmlr.getText());
            System.out.print("-->");
            break;

        case XMLStreamConstants.ENTITY_REFERENCE:
            System.out.print(xmlr.getLocalName() + "=");
            if (xmlr.hasText())
                System.out.print("[" + xmlr.getText() + "]");
            break;

        case XMLStreamConstants.START_DOCUMENT:
            System.out.print("<?xml");
            System.out.print(" version='" + xmlr.getVersion() + "'");
            System.out.print(" encoding='" + xmlr.getCharacterEncodingScheme() + "'");
            if (xmlr.isStandalone())
                System.out.print(" standalone='yes'");
            else
                System.out.print(" standalone='no'");
            System.out.print("?>");
            break;

        }
        System.out.println("]");
    }

    private static void printName(XMLStreamReader xmlr) {
        if (xmlr.hasName()) {
            String prefix = xmlr.getPrefix();
            String uri = xmlr.getNamespaceURI();
            String localName = xmlr.getLocalName();
            printName(prefix, uri, localName);
        }
    }

    private static void printName(String prefix, String uri, String localName) {
        if (uri != null && !("".equals(uri)))
            System.out.print("['" + uri + "']:");
        if (prefix != null)
            System.out.print(prefix + ":");
        if (localName != null)
            System.out.print(localName);
    }

    private static void printAttributes(XMLStreamReader xmlr) {
        for (int i = 0; i < xmlr.getAttributeCount(); i++) {
            printAttribute(xmlr, i);
        }
    }

    private static void printAttribute(XMLStreamReader xmlr, int index) {
        String prefix = xmlr.getAttributePrefix(index);
        String namespace = xmlr.getAttributeNamespace(index);
        String localName = xmlr.getAttributeLocalName(index);
        String value = xmlr.getAttributeValue(index);
        System.out.print(" ");
        printName(prefix, namespace, localName);
        System.out.print("='" + value + "'");
    }

    private static void printNamespaces(XMLStreamReader xmlr) {
        for (int i = 0; i < xmlr.getNamespaceCount(); i++) {
            printNamespace(xmlr, i);
        }
    }

    private static void printNamespace(XMLStreamReader xmlr, int index) {
        String prefix = xmlr.getNamespacePrefix(index);
        String uri = xmlr.getNamespaceURI(index);
        System.out.print(" ");
        if (prefix == null)
            System.out.print("xmlns='" + uri + "'");
        else
            System.out.print("xmlns:" + prefix + "='" + uri + "'");
    }

    private void restoreEntry(XMLStreamReader reader, OrtolangImportExportLogger logger) throws Exception {
        LOGGER.log(Level.FINE, "restoring entry");

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
