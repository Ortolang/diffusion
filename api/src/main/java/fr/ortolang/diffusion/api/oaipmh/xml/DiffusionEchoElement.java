package fr.ortolang.diffusion.api.oaipmh.xml;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
