package fr.ortolang.diffusion.dump;

import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XmlDumpHelper {
    
    public static String namespace = "http://www.ortolang.fr";
    
    public static void startDocument(String prefix, String name, XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument("utf-8", "1.0");
        writer.setPrefix(prefix, namespace);
        writer.writeStartElement(namespace, name);
        writer.writeNamespace(prefix, namespace);
        if ( attrs != null ) {
            for ( Entry<String, String> attr : attrs.entrySet() ) {
                writer.writeAttribute(attr.getKey(), attr.getValue());
            }
        }
    }
    
    public static void outputEmptyElement(String prefix, String name, XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        startElement(prefix, name, attrs, writer);
        endElement(writer);
    }
    
    public static void outputElementWithContent(String prefix, String name, XmlDumpAttributes attrs, String content, XMLStreamWriter writer) throws XMLStreamException {
        startElement(prefix, name, attrs, writer);
        writer.writeCharacters(content);
        endElement(writer);
    }
    
    public static void startElement(String prefix, String name, XmlDumpAttributes attrs, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(prefix, name, namespace);
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
