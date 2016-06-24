package fr.ortolang.diffusion.extraction.parser;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrtolangXMLParser extends CompositeParser {

    private static final Logger LOGGER = Logger.getLogger(OrtolangXMLParser.class.getName());

    public static final String xmlTypeKey = "XML-Type";

    public enum XMLType {
        TEI,
        TRANS
    }

    private static final List<Parser> xmlParsers = Arrays.asList(new Parser[]{
            new TeiParser(), new TransParser()
    });

    private static DocumentBuilderFactory documentBuilderFactory;

    static {
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public OrtolangXMLParser() {
        super(new MediaTypeRegistry(), xmlParsers);
    }

    @Override
    public Map<MediaType, Parser> getParsers(ParseContext context) {
        OrtolangContext ortolangContext = context.get(OrtolangContext.class);
        if (ortolangContext != null) {
            Map<MediaType, Parser> map = new HashMap<>();
            switch (ortolangContext.getType()) {
            case TEI:
                map.put(getMediaTypeRegistry().normalize(MediaType.application("xml")), new TeiParser());
                break;
            case TRANS:
                map.put(getMediaTypeRegistry().normalize(MediaType.application("xml")), new TransParser());
                break;
            }
            return map;
        }
        return super.getParsers(context);
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(stream);
            OrtolangContext ortolangContext = null;
            if (doc.getDocumentElement().hasAttribute("xmlns") && "http://www.tei-c.org/ns/1.0".equals(doc.getDocumentElement().getAttribute("xmlns"))) {
                 ortolangContext = new OrtolangContext(XMLType.TEI);
            } else if (doc.getDoctype() != null && "Trans".equals(doc.getDoctype().getName())) {
                 ortolangContext = new OrtolangContext(XMLType.TRANS);
            }
            if (ortolangContext != null) {
                context.set(OrtolangContext.class, ortolangContext);
                context.set(Document.class, doc);
                metadata.add("XML-Version", doc.getXmlVersion());
                if (doc.getXmlEncoding() != null) {
                    metadata.add(Metadata.CONTENT_ENCODING, doc.getXmlEncoding());
                }
                super.parse(stream, handler, metadata, context);
            }
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred while parsing stream");
        }
    }

    public class OrtolangContext {
        private XMLType type;

        public OrtolangContext(XMLType type) {
            this.type = type;
        }

        public XMLType getType() {
            return type;
        }

        public void setType(XMLType type) {
            this.type = type;
        }
    }
}
