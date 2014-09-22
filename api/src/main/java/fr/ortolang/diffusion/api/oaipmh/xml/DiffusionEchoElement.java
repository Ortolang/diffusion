package fr.ortolang.diffusion.api.oaipmh.xml;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLInputFactory2;

import com.lyncode.xml.XmlWritable;
import com.lyncode.xml.XmlWriter;
import com.lyncode.xml.exceptions.XmlWriteException;

/**
 * Same as EchoElement from XOAI but add namespace in element when specify from the XML source file.
 * 
 * @author cyril
 *
 */
public class DiffusionEchoElement implements XmlWritable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private static XMLInputFactory factory = XMLInputFactory2.newFactory();
	private String xmlString = null;
	private List<String> declaredPrefixes = new ArrayList<String>();

	public DiffusionEchoElement(String xmlString) {
		this.xmlString = xmlString;
	}

	@Override
	public void write(XmlWriter writer) throws XmlWriteException {
		try {
			XMLEventReader reader = factory.createXMLEventReader(new ByteArrayInputStream(xmlString.getBytes()));
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				
				if (event.isStartElement()) {
					
					QName name = event.asStartElement().getName();
					logger.log(Level.FINEST, "write xml element "+name.getLocalPart());
					
					writer.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());
					addNamespaceIfRequired(writer, name);

					@SuppressWarnings("rawtypes")
					Iterator ns = event.asStartElement().getNamespaces();
					while(ns.hasNext()) {
						Object o = ns.next();
						
						if(o instanceof Namespace) {
							logger.log(Level.FINEST, "Try to add namespace "+((Namespace) o).getName());
							addNamespaceIfRequired(writer, new QName(((Namespace) o).getNamespaceURI(), "", ((Namespace) o).getPrefix()));
						}
					}
					
					@SuppressWarnings("unchecked")
					Iterator<Attribute> it = event.asStartElement().getAttributes();

					while (it.hasNext()) {
						Attribute attr = it.next();
						QName attrName = attr.getName();
						addNamespaceIfRequired(writer, attrName);
						writer.writeAttribute(attrName.getPrefix(), attrName.getNamespaceURI(), attrName.getLocalPart(), attr.getValue());
					}
				} else if (event.isEndElement()) {
					writer.writeEndElement();
				} else if (event.isCharacters()) {
					writer.writeCharacters(event.asCharacters().getData());
				} else {
					logger.log(Level.FINEST, "write unknown event "+event.getEventType());
					
				}
			}
		} catch (XMLStreamException e) {
			throw new XmlWriteException("Error trying to output '"+this.xmlString+"'", e);
		}
	}

	private void addNamespaceIfRequired(XmlWriter writer, QName name) throws XMLStreamException {
		if (!declaredPrefixes.contains(name.getPrefix())) {
			writer.writeNamespace(name.getPrefix(), name.getNamespaceURI());
			declaredPrefixes.add(name.getPrefix());
		}
	}
}
