package fr.ortolang.diffusion.oai.format.builder;

import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class XMLMetadataBuilder implements MetadataBuilder {

	private XMLStreamWriter writer;
	private XmlDumpNamespaces namespaces;

	public XMLMetadataBuilder(XMLStreamWriter writer) {
		this(writer, null);
	}

	public XMLMetadataBuilder(XMLStreamWriter writer, XmlDumpNamespaces namespaces) {
		this.writer = writer;
		this.namespaces = namespaces;
	}

	@Override
	public void writeStartDocument(String ns, String name, XmlDumpAttributes attrs) throws MetadataBuilderException {
		try {
			writer.writeStartDocument("utf-8", "1.0");
			String uri = namespaces!=null ? namespaces.get(ns).getUri() : null;
			if (uri!=null) {
				writer.writeStartElement(ns, name, uri);
			} else {
				writer.writeStartElement(name);
			}
			boolean xsiNamespace = false;
			StringBuilder schemaLocationValue = new StringBuilder();
			if ( namespaces != null) {
				for ( Entry<String, XmlDumpNamespace> namespace : namespaces.entrySet() ) {
	                writer.writeNamespace(namespace.getKey(), namespace.getValue().getUri());
	                if (namespace.getValue().getSchemaLocation() != null) {
	                	schemaLocationValue.append(namespace.getValue().getUri()).append(" ")
	                		.append(namespace.getValue().getSchemaLocation()).append(" ");
	                }
	                if (namespace.getValue().getUri().equals(Constant.XSI_NAMESPACE_URI)) {
	                	xsiNamespace = true;
	                }
	            }
			}
			if ( xsiNamespace ) {
				writer.writeAttribute(Constant.XSI_SCHEMA_LOCATION, schemaLocationValue.toString());
			}
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
	public void writeStartElement(String ns, String name, XmlDumpAttributes attrs) throws MetadataBuilderException {
		writeStartElement(ns, name, attrs, null);
	}
	
	@Override
	public void writeStartElement(String ns, String name, XmlDumpAttributes attrs, String value) throws MetadataBuilderException {
		try {
			String uri = namespaces!=null ? namespaces.get(ns).getUri() : null;
			if (uri != null) {
				writer.writeStartElement(ns, name, uri);
			} else {
				writer.writeStartElement(name);
			}
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

	public XmlDumpNamespaces getNamespaces() {
		return namespaces;
	}

	@Override
	public void setNamespaces(XmlDumpNamespaces namespaces) {
		this.namespaces = namespaces;
	}
}
