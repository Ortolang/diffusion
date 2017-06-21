package fr.ortolang.diffusion.oai.format;

import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;

public class XMLMetadataBuilder implements MetadataBuilder {

	private XMLStreamWriter writer;
	
	public XMLMetadataBuilder(XMLStreamWriter writer) {
		this.writer = writer;
	}

	@Override
	public void writeStartDocument(String ns, String name, XmlDumpAttributes attrs) throws MetadataBuilderException {
		try {
			writer.writeStartDocument("utf-8", "1.0");
		
//        writer.setPrefix(ORTOLANG_PREFIX, ORTOLANG_NS);
        writer.writeStartElement(name);
//        writer.writeNamespace(ORTOLANG_PREFIX, ORTOLANG_NS);
//        if ( attrs != null ) {
//            for ( Entry<String, String> attr : attrs.entrySet() ) {
//                writer.writeAttribute(attr.getKey(), attr.getValue());
//            }
//        }
		} catch (XMLStreamException e) {
			throw new MetadataBuilderException(e.getMessage(), e);
		}
	}

	@Override
	public void writeEndDocument() throws MetadataBuilderException {
		try {
			writer.writeEndDocument();
		} catch (XMLStreamException e) {
			throw new MetadataBuilderException(e.getMessage(), e);
		}
	}

	@Override
	public void writeStartElement(String ns, String name, XmlDumpAttributes attrs, String value) throws MetadataBuilderException {
		try {
	//		writer.writeStartElement(ORTOLANG_PREFIX, name, ORTOLANG_NS);
			writer.writeStartElement(name);
	        if ( attrs != null ) {
	            for ( Entry<String, String> attr : attrs.entrySet() ) {
	                writer.writeAttribute(attr.getKey(), attr.getValue());
	            }
	        }
	        if ( value != null) {
	        	writer.writeCharacters(value);
	        }
		} catch (XMLStreamException e) {
			throw new MetadataBuilderException(e.getMessage(), e);
		}
	}
	
	@Override
	public void writeStartElement(String ns, String name, String value) throws MetadataBuilderException {
		writeStartElement(ns, name, null, value);
	}

	@Override
	public void writeStartEndElement(String ns, String name, XmlDumpAttributes attrs, String value) throws MetadataBuilderException {
		writeStartElement(ns, name, attrs, value);
		writeEndElement();
	}

	@Override
	public void writeStartEndElement(String ns, String name, String value) throws MetadataBuilderException {
		writeStartElement(ns, name, null, value);
		writeEndElement();
	}

	@Override
	public void writeEndElement() throws MetadataBuilderException {
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			throw new MetadataBuilderException(e.getMessage(), e);
		}
	}
	
	public String toString() {
		return null;
	}
}
