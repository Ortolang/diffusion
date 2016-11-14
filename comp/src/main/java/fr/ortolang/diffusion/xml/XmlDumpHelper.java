package fr.ortolang.diffusion.xml;

import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XmlDumpHelper {
    
    public static final String ORTOLANG_PREFIX = "ortolang";
    public static final String ORTOLANG_NS = "http://www.ortolang.fr/";
    
    public static void startDocument(XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument("utf-8", "1.0");
        writer.setPrefix(ORTOLANG_PREFIX, ORTOLANG_NS);
        writer.writeStartElement(ORTOLANG_NS, "ortolang-dump");
        writer.writeNamespace(ORTOLANG_PREFIX, ORTOLANG_NS);
        if ( attrs != null ) {
            for ( Entry<String, String> attr : attrs.entrySet() ) {
                writer.writeAttribute(attr.getKey(), attr.getValue());
            }
        }
    }
    
    public static void outputEmptyElement(String name, XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        startElement(name, attrs, writer);
        endElement(writer);
    }
    
    public static void outputElementWithContent(String name, XmlDumpAttributes attrs, String content, XMLStreamWriter writer) throws XMLStreamException {
        startElement(name, attrs, writer);
        writer.writeCharacters(content);
        endElement(writer);
    }
    
    public static void outputElementWithData(String name, XmlDumpAttributes attrs, String data, XMLStreamWriter writer) throws XMLStreamException {
        startElement(name, attrs, writer);
        writer.writeCData(data);
        endElement(writer);
    }
    
    public static void startElement(String name, XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(ORTOLANG_PREFIX, name, ORTOLANG_NS);
        if ( attrs != null ) {
            for ( Entry<String, String> attr : attrs.entrySet() ) {
                writer.writeAttribute(attr.getKey(), attr.getValue());
            }
        }
    }
    
    public static void endElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement();
    }
    
    public static void endDocument(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndDocument();
    }

}
