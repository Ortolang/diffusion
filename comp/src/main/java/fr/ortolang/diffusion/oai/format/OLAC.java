package fr.ortolang.diffusion.oai.format;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

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

	public static final String DCTERMS_NAMESPACE = "dcterms";
	public static final String OLAC_NAMESPACE = "olac";
	public static final List<String> DCTERMS_ELEMENTS = Arrays.asList("abstract", "accessRights", "accrualMethod", "accrualPeriodicity", "accrualPolicy",
    		"alternative", "audience", "available", "bibliographicCitation", "conformsTo", "created", "dateAccepted", "dateCopyrighted",
    		"dateSubmitted", "educationLevel", "extent", "hasFormat", "hasPart", "hasVersion", "instructionalMethod", "isFormatOf", 
    		"isPartOf", "isReferencedBy", "isReplacedBy", "isRequiredBy", "issued", "isVersionOf", "license", "mediator", "medium",
    		"modified", "provenance", "references", "replaces", "requires", "rightsHolder", "spatial", "tableOfContents", "temporal",
    		"valid");
	public static HashMap<List<String>, String> OLAC_TO_DC_ELEMENTS = new HashMap<List<String>, String>();
	static {
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("alternative"), "title");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("tableOfContents", "abstract"), "description");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("created", "valid", "available", "issued", "modified", "dateAccepted", "dateCopyrighted", "dateSubmitted"), "date");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("extent", "medium"), "format");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("bibliographicCitation"), "identifier");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("isVersionOf", "hasVersion", "isReplacedBy", "replaces", "isRequiredBy", "requires", "isPartOf", "hasPart", "isReferencedBy", "references", "isFormatOf", "hasFormat", "conformsTo"), "relation");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("spatial", "temporal"), "coverage");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("accessRights", "license"), "rights");
	}
	
	public static final List<String> DATE_ELEMENTS = Arrays.asList("date", "issued", "dateCopyrighted", "created", "available", "dateAccepted", "dateSubmitted", "modified", "valid");
    
    public OLAC() {
        header = "<olac:olac xmlns:olac=\"http://www.language-archives.org/OLAC/1.1/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.language-archives.org/OLAC/1.1/ http://www.language-archives.org/OLAC/1.1/olac.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd\">";
        footer = "</olac:olac>";
    }

    public void addDctermsField(String name, String value) {
    	fields.add(XMLElement.createElement(DCTERMS_NAMESPACE, name, value));
    }

    public void addDctermsField(String name, String xsitype, String value) {
    	fields.add(XMLElement.createElement(DCTERMS_NAMESPACE, name, value).withAttribute("xsi:type", xsitype));
    }

    public void addDctermsMultilingualField(String name, String lang, String value) {
    	fields.add(XMLElement.createElement(DCTERMS_NAMESPACE, name, value).withAttribute("xml:lang", lang));
    }
    
    public void addOlacField(String name, String xsitype, String olaccode) {
    	addOlacField(name, xsitype, olaccode, null);
    }

    public void addOlacField(String name, String xsitype, String olaccode, String value) {
    	fields.add(XMLElement.createElement(OLAC_NAMESPACE, name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode));
    }

    public void addOlacField(String name, String xsitype, String olaccode, String lang, String value) {
    	fields.add(XMLElement.createElement(OLAC_NAMESPACE, name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode).withAttribute("xml:lang", lang));
    }

    public OLAC addOlacElement(String elementName, JsonObject meta) {
    	if (meta.containsKey(elementName)) {
    		JsonArray elementArray = meta.getJsonArray(elementName);
            for(JsonObject elementObject : elementArray.getValuesAs(JsonObject.class)) {
            	XMLElement elementXml = XMLElement.createElement(DC_NAMESPACE, elementName);
            	
    			if (elementObject.containsKey("value")) {
    				elementXml.setValue(XMLDocument.removeHTMLTag(elementObject.getString("value")));
    			} 
            	if (elementObject.containsKey("lang")) {
            		elementXml.withAttribute("xml:lang", elementObject.getString("lang"));
            	} 
            	if (elementObject.containsKey("type")) {
            		elementXml.withAttribute("xsi:type", elementObject.getString("type"));
            	}
            	if (elementObject.containsKey("code")) {
            		elementXml.withAttribute("olac:code", elementObject.getString("code"));
            	}
            	fields.add(elementXml);
            }
    	}
    	return this;
    }
    
    public OLAC addDctermsElement(String elementName, JsonObject meta) {
    	if (meta.containsKey(elementName)) {
    		JsonArray elementArray = meta.getJsonArray(elementName);
    		for(JsonObject elementObject : elementArray.getValuesAs(JsonObject.class)) {
    			XMLElement elementXml = XMLElement.createElement(DCTERMS_NAMESPACE, elementName, XMLDocument.removeHTMLTag(elementObject.getString("value")));
    			
    			if (elementObject.containsKey("lang")) {
    				elementXml.withAttribute("xml:lang", elementObject.getString("lang"));
    			} 
    			if (elementObject.containsKey("type")) {
    				elementXml.withAttribute("xsi:type", elementObject.getString("type"));
    			}
    			fields.add(elementXml);
    		}
    	}
    	return this;
    }
    
}
