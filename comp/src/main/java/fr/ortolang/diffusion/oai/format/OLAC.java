package fr.ortolang.diffusion.oai.format;

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

public class OLAC extends DCXMLDocument {

    public OLAC() {
        header = "<olac:olac xmlns:olac=\"http://www.language-archives.org/OLAC/1.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.language-archives.org/OLAC/1.1/ http://www.language-archives.org/OLAC/1.1/olac.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd\">";
        footer = "</olac:olac>";
    }

    public void addDctermsField(String name, String xsitype, String value) {
    	fields.add(XMLElement.createDctermsElement(name, value).withAttribute("xsi:type", xsitype));
    }

    public void addDctermsMultilingualField(String name, String lang, String value) {
    	fields.add(XMLElement.createDctermsElement(name, value).withAttribute("xml:lang", lang));
    }
    
    public void addOlacField(String name, String xsitype, String olaccode) {
    	addOlacField(name, xsitype, olaccode, null);
    }

    public void addOlacField(String name, String xsitype, String olaccode, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode));
    }

    public void addOlacField(String name, String xsitype, String olaccode, String lang, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode).withAttribute("xml:lang", lang));
    }
}
