package fr.ortolang.diffusion.parser;

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

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.xml.XMLParser;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeiParser extends XMLParser {

    private static final Logger LOGGER = Logger.getLogger(TeiParser.class.getName());

    private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.application("xml"));

    private static DocumentBuilderFactory documentBuilderFactory;

    private static String xslStylesheetString;

    static {
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            InputStream xslStylesheetStream = TeiParser.class.getClassLoader().getResourceAsStream("xsl/teiHeader2OrtolangMD.xsl");
            try (Scanner s = new Scanner(xslStylesheetStream)) {
                xslStylesheetString = s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            StringReader stringReader = new StringReader(xslStylesheetString);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(stream);
            if (doc.getDocumentElement().hasAttribute("xmlns") && doc.getDocumentElement().getAttribute("xmlns").equals("http://www.tei-c.org/ns/1.0")) {
                Transformer transformer = transformerFactory.newTransformer(new StreamSource(stringReader));
                try (OutputStream outputStream = new ByteArrayOutputStream()) {
                    transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
                    metadata.add("ortolang:json", outputStream.toString());
                    metadata.add("XML-Type", "TEI");
                }
            }

        } catch (TransformerException | ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
