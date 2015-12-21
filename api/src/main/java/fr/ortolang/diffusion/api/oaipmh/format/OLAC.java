package fr.ortolang.diffusion.api.oaipmh.format;

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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

public class OLAC extends OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OLAC.class.getName());

    public OLAC() {
        super();
        
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

    public static OLAC valueOf(JsonObject doc) {
    	OLAC olac = new OLAC();

        // Identifier
        //TODO Mettre le handle

        // Différence avec OAI_DC ?
        // TODO : Contributor OLAC avec xsi:type + olac:code
        // Subject olac:code
        // TODO Language olac:code + code ISO
        
        // Plus
        // dcterms provenance ? ORTOLANG ?

        try {
            JsonArray multilingualTitles = doc.getJsonArray("meta_ortolang-item-jsontitle");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
            	olac.addDcMultilingualField("title", multilingualTitle.getString("lang"), multilingualTitle.getString("value"));
            }

            JsonArray corporaLanguages = doc.getJsonArray("meta_ortolang-item-jsoncorporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonString corporaLanguage : corporaLanguages.getValuesAs(JsonString.class)) {
                	olac.addDcMultilingualField("subject", "fr", corporaLanguage.getString());
                	olac.addDcMultilingualField("language", "fr", corporaLanguage.getString());
//                    olac.setOlacField("language", "olac:language", corporaLanguage.getString());
                    //TODO mettre le code ISO ?
                }
            }
            
            JsonArray multilingualKeywords = doc.getJsonArray("meta_ortolang-item-jsonkeywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                	olac.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }

            JsonArray contributors = doc.getJsonArray("meta_ortolang-item-jsoncontributors");
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("role");
                for(JsonString role : roles.getValuesAs(JsonString.class)) {
                    if(role.getString().equals("producer")) {
                        JsonObject entityContributor = contributor.getJsonObject("entity");
                        String fullname = entityContributor.getString("fullname");
                        olac.addDcField("publisher", fullname);
                    } else {
                    	olac.addOlacField("contributor", "olac:role", role.getString(), contributor(contributor));
                    }
                    
                    if(role.getString().equals("author")) {
                    	olac.addDcField("creator", creator(contributor));
                    }
                }
            }

            JsonString statusOfUse = doc.getJsonString("meta_ortolang-item-jsonstatusOfUse");
            if(statusOfUse!=null) {
            	olac.addDcField("rights", statusOfUse.getString());
            }
            JsonString conditionsOfUse = doc.getJsonString("meta_ortolang-item-jsonconditionsOfUse");
            if(conditionsOfUse!=null) {
                olac.addDcField("rights", conditionsOfUse.getString());
            }
            JsonString licenseWebsite = doc.getJsonString("meta_ortolang-item-jsonlicenseWebsite");
            if(licenseWebsite!=null) {
            	//TODO check for xsi:type DCTERMS:URI
            	olac.addDctermsField("licence", "dcterms:URI", licenseWebsite.getString());
            }

            JsonString linguisticDataType = doc.getJsonString("meta_ortolang-item-jsonlinguisticDataType");
            if(linguisticDataType!=null) {
                olac.addOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
            }
            JsonArray discourseTypes = doc.getJsonArray("meta_ortolang-item-jsondiscourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                    olac.addOlacField("type", "olac:discourse-type", discourseType.getString());
                }
            }
            JsonArray linguisticSubjects = doc.getJsonArray("meta_ortolang-item-jsonlinguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                    olac.addOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
                }
            }

            JsonArray bibligraphicCitations = doc.getJsonArray("meta_ortolang-item-jsonbibliographicCitation");
            if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	                olac.addDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
	            }
            }
            JsonString publicationDate = doc.getJsonString("meta_ortolang-item-jsonpublicationDate");
            JsonString creationDate = doc.getJsonString("meta_ortolang-item-jsoncreationDate");
            if(creationDate!=null) {
                olac.addDcField("date", creationDate.getString());
              //TODO check date validation and convert
                olac.addDctermsField("temporal", "dcterms:W3CDTF", creationDate.getString());
            } else {
                if(publicationDate!=null) {
                    olac.addDcField("date", publicationDate.getString());
                }
            }
            if(publicationDate!=null) {
                //TODO get created date if publicationDate not set
                olac.addDctermsField("created", "dcterms:W3CDTF", publicationDate.getString());
            }
            JsonNumber lastModificationDate = doc.getJsonNumber("lastModificationDate");
            Long longTimestamp = Long.valueOf(lastModificationDate.longValue());
            Date datestamp = new Date(longTimestamp);
            olac.addDctermsField("modified", "dcterms:W3CDTF", w3cdtf.format(datestamp));
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
    	return olac;
    }
}
