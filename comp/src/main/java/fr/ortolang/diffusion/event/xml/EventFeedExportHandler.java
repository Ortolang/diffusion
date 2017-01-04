package fr.ortolang.diffusion.event.xml;

import java.util.Collections;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.event.entity.EventFeed;
import fr.ortolang.diffusion.event.entity.EventFeedFilter;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpHelper;

public class EventFeedExportHandler implements OrtolangObjectXmlExportHandler {
        
        private EventFeed feed; 
        
        public EventFeedExportHandler(EventFeed feed) {
            this.feed = feed;
       }
        
        @Override
        public void exportObject(XMLStreamWriter writer, OrtolangImportExportLogger logger) throws OrtolangException {
            try {
                XmlDumpAttributes attrs = new XmlDumpAttributes();
                attrs.put("id", feed.getId());
                attrs.put("name", feed.getName());
                attrs.put("description", feed.getDescription());
                XmlDumpHelper.startElement("feed", attrs, writer);
                
                attrs = new XmlDumpAttributes();
                XmlDumpHelper.startElement("feed-filters", attrs, writer);
                for ( EventFeedFilter filter : feed.getFilters() ) {
                    attrs = new XmlDumpAttributes();
                    attrs.put("id", filter.getId());
                    attrs.put("event-type", filter.getEventTypeRE());
                    attrs.put("from-object", filter.getFromObjectRE());
                    attrs.put("object-type", filter.getObjectTypeRE());
                    attrs.put("throwed-by", filter.getThrowedByRE());
                    XmlDumpHelper.outputEmptyElement("feed-filter", attrs, writer);
                }
                XmlDumpHelper.endElement(writer);
                
                XmlDumpHelper.endElement(writer);
            } catch ( XMLStreamException e ) {
                throw new OrtolangException("error during dumping event feed", e);
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
