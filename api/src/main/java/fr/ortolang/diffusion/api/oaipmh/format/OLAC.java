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

    public void addOlacField(String name, String xsitype, String olaccode, String lang, String value) {
    	fields.add(XMLElement.createDcElement(name, value).withAttribute("xsi:type", xsitype).withAttribute("olac:code", olaccode).withAttribute("xml:lang", lang));
    }

    public static OLAC valueOf(JsonObject doc, JsonObject workspaceDoc) {
    	OLAC olac = new OLAC();

        // Identifier
        JsonObject workspace = doc.getJsonObject("meta_ortolang-workspace-json");
//        String snapshotName = workspace.getString("snapshotName");
//        JsonObject workspaceMeta = workspaceDoc.getJsonObject("meta_ortolang-workspace-json");
//        JsonArray tags = workspaceMeta.getJsonArray("tags");
//        if(tags!=null) {
//        	for(JsonObject tag : tags.getValuesAs(JsonObject.class)) {
//        		if(tag.getString("snapshot").equals(snapshotName)) {
//        			olac.addDctermsField("identifier", "dcterms:URI", identifier(workspace.getString("wsalias"),  tag.getString("name")));
//        		}
//        	}
//        }
        olac.addDctermsField("identifier", "dcterms:URI", identifier(workspace.getString("wsalias")));
        
        try {
        	JsonObject meta = doc.getJsonObject("meta_ortolang-item-json");
        	
	            JsonArray multilingualTitles = meta.getJsonArray("title");
	            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
	            	olac.addDcMultilingualField("title", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
	            }

	            JsonArray multilingualDescriptions = meta.getJsonArray("description");
	            for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
	            	olac.addDcMultilingualField("description", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
	            }

	            JsonArray corporaLanguages = meta.getJsonArray("corporaLanguages");
	            if(corporaLanguages!=null) {
	                for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
	                	JsonObject metaLanguage = corporaLanguage.getJsonObject("meta_ortolang-referential-json");
	                	JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");
	                	
	                	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	                		olac.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//	                		olac.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
		                	olac.addOlacField("language", "olac:language", metaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
	                	}
	                }
	            }

	            JsonArray studyLanguages = meta.getJsonArray("studyLanguages");
	            if(studyLanguages!=null) {
	                for(JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
	                	JsonObject metaLanguage = studyLanguage.getJsonObject("meta_ortolang-referential-json");
	                	JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");
	                	
	                	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//	                		olac.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
//	                		olac.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
		                	olac.addOlacField("subject", "olac:language", metaLanguage.getString("id"), label.getString("lang"), label.getString("value"));
	                	}
	                }
	            }
	            
	            JsonArray multilingualKeywords = meta.getJsonArray("keywords");
	            if(multilingualKeywords!=null) {
	                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
	                	olac.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
	                }
	            }
	
	            JsonArray contributors = meta.getJsonArray("contributors");
	            if(contributors!=null) {
		            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
		                JsonArray roles = contributor.getJsonArray("roles");
		                for(JsonObject role : roles.getValuesAs(JsonObject.class)) {
		                	JsonObject metaRole = role.getJsonObject("meta_ortolang-referential-json");
	    					String roleId = metaRole.getString("id");
		                    
		                    olac.addOlacField("contributor", "olac:role", roleId, person(contributor));
		                    
		                    if(roleId.equals("author")) {
		                    	olac.addDcField("creator", person(contributor));
		                    }
		                }
		            }
	            }

	            JsonArray producers = meta.getJsonArray("producers");
	            if(producers!=null) {
	            	for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
		            	JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");
		            	
		            	if(metaOrganization.containsKey("fullname")) {
		            		olac.addDcField("publisher", metaOrganization.getString("fullname"));
		            	}
		            }
	            }

	            JsonArray sponsors = meta.getJsonArray("sponsors");
	            if(sponsors!=null) {
	            	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
		            	JsonObject metaOrganization = sponsor.getJsonObject("meta_ortolang-referential-json");
		            	
		            	if(metaOrganization.containsKey("fullname")) {
		            		olac.addOlacField("contributor", "olac:role", "sponsor", metaOrganization.getString("fullname"));
		            	}
		            }
	            }
	            
	            JsonObject statusOfUse = meta.getJsonObject("statusOfUse");
	            if(statusOfUse!=null) {
	            	JsonObject metaStatusOfUse = statusOfUse.getJsonObject("meta_ortolang-referential-json");
	            	String idStatusOfUse = metaStatusOfUse.getString("id");
	            	olac.addDcField("rights", idStatusOfUse);
	                
	                JsonArray multilingualLabels = metaStatusOfUse.getJsonArray("labels");
                	for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                		olac.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
                	}
	            }
	            JsonArray conditionsOfUse = meta.getJsonArray("conditionsOfUse");
	            if(conditionsOfUse!=null) {
	            	for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
                		olac.addDcMultilingualField("rights", label.getString("lang"), removeHTMLTag(label.getString("value")));
                	}
	            }
	            
	            JsonObject license = meta.getJsonObject("license");
	            if(license!=null) {
	            	JsonObject metaLicense = license.getJsonObject("meta_ortolang-referential-json");
	            	if(metaLicense!=null) {
	            		olac.addDctermsMultilingualField("license", "fr", metaLicense.getString("label"));
	            	}
	            }
	
	            JsonString linguisticDataType = meta.getJsonString("linguisticDataType");
	            if(linguisticDataType!=null) {
	                olac.addOlacField("type", "olac:linguistic-type", linguisticDataType.getString());
	            }
	            JsonArray discourseTypes = meta.getJsonArray("discourseTypes");
	            if(discourseTypes!=null) {
	                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
	                    olac.addOlacField("type", "olac:discourse-type", discourseType.getString());
	                }
	            }
	            JsonArray linguisticSubjects = meta.getJsonArray("linguisticSubjects");
	            if(linguisticSubjects!=null) {
	                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
	                    olac.addOlacField("subject", "olac:linguistic-field", linguisticSubject.getString());
	                }
	            }
	
	            JsonArray bibligraphicCitations = meta.getJsonArray("bibliographicCitation");
	            if(bibligraphicCitations!=null) {
		            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
		                olac.addDctermsMultilingualField("bibliographicCitation", multilingualBibliographicCitation.getString("lang"), multilingualBibliographicCitation.getString("value"));
		            }
	            }
	            JsonString publicationDate = meta.getJsonString("publicationDate");
	            JsonString creationDate = meta.getJsonString("originDate");
	            if(creationDate!=null) {
	                olac.addDcField("date", creationDate.getString());
	              //TODO check date validation and convert
	                olac.addDctermsField("temporal", "dcterms:W3CDTF", creationDate.getString());
	            } else {
	                if(publicationDate!=null) {
	                    olac.addDcField("date", publicationDate.getString());
	                }
	            }
	            JsonNumber lastModificationDate = doc.getJsonNumber("lastModificationDate");
	            Long longTimestamp = lastModificationDate.longValue();
	            Date datestamp = new Date(longTimestamp);
	            olac.addDctermsField("modified", "dcterms:W3CDTF", w3cdtf.format(datestamp));
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }
        
    	return olac;
    }
}
