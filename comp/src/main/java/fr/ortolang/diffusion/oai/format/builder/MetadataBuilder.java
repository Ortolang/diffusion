package fr.ortolang.diffusion.oai.format.builder;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public interface MetadataBuilder {

	void setNamespaces(XmlDumpNamespaces namespaces);
	void writeStartDocument(String ns, String name, XmlDumpAttributes attrs) throws MetadataBuilderException;
	void writeEndDocument() throws MetadataBuilderException;
	void writeStartElement(String ns, String name) throws MetadataBuilderException;
	void writeStartElement(String ns, String name, XmlDumpAttributes attrs) throws MetadataBuilderException;
	void writeStartElement(String ns, String name, XmlDumpAttributes attrs, String value) throws MetadataBuilderException;
	void writeStartElement(String ns, String name, String value) throws MetadataBuilderException;
	void writeStartEndElement(String ns, String name) throws MetadataBuilderException;
	void writeStartEndElement(String ns, String name, XmlDumpAttributes attrs, String value) throws MetadataBuilderException;
	void writeStartEndElement(String ns, String name, String value) throws MetadataBuilderException;
	void writeEndElement() throws MetadataBuilderException;
	String toString();
}
