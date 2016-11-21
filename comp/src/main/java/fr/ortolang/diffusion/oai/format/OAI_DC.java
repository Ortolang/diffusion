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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.core.entity.MetadataFormat;

public class OAI_DC extends XMLWriter {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());
    protected static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");

    public OAI_DC() {
    	
    }
    
    /**
     * Writes DublinCore informations from a workspace.
     * @param doc
     * @param oaiDc
     */
    private void fromWorkspace(JsonObject doc) {
    	
    	JsonObject meta = doc.getJsonObject("meta_ortolang-item-json");
    	
        JsonArray multilingualTitles = meta.getJsonArray("title");
        for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
            this.addDcMultilingualField("title", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
        }

        JsonArray multilingualDescriptions = meta.getJsonArray("description");
        for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
            this.addDcMultilingualField("description", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
        }

        JsonArray corporaLanguages = meta.getJsonArray("corporaLanguages");
        if(corporaLanguages!=null) {
            for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
                JsonObject metaLanguage = corporaLanguage.getJsonObject("meta_ortolang-referential-json");
                JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");

                for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                    this.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
                    this.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
                }
                this.addDcField("language", metaLanguage.getString("id"));
            }
        }

        JsonArray multilingualKeywords = meta.getJsonArray("keywords");
        if(multilingualKeywords!=null) {
            for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                this.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
            }
        }

        JsonArray producers = meta.getJsonArray("producers");
        if(producers!=null) {
            for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
                JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");

                if(metaOrganization.containsKey("fullname")) {
                    this.addDcField("publisher", metaOrganization.getString("fullname"));
                }
            }
        }

        JsonArray contributors = meta.getJsonArray("contributors");
        if(contributors!=null) {
            for(JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
                JsonArray roles = contributor.getJsonArray("roles");
                for(JsonObject role : roles.getValuesAs(JsonObject.class)) {
                    JsonObject metaRole = role.getJsonObject("meta_ortolang-referential-json");
                    String roleId = metaRole.getString("id");
                    this.addDcField("contributor", person(contributor)+" ("+roleId+")");

                    if("author".equals(roleId)) {
                        this.addDcField("creator", person(contributor));
                    }
                }
            }
        }

        JsonObject statusOfUse = meta.getJsonObject("statusOfUse");
        if(statusOfUse!=null) {
            JsonObject metaStatusOfUse = statusOfUse.getJsonObject("meta_ortolang-referential-json");
            String idStatusOfUse = metaStatusOfUse.getString("id");
            this.addDcField("rights", idStatusOfUse);

            JsonArray multilingualLabels = metaStatusOfUse.getJsonArray("labels");
            for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                this.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
            }
        }
        JsonArray conditionsOfUse = meta.getJsonArray("conditionsOfUse");
        if(conditionsOfUse!=null) {
            for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
                this.addDcMultilingualField("rights", label.getString("lang"), removeHTMLTag(label.getString("value")));
            }
        }

        JsonObject license = meta.getJsonObject("license");
        if(license!=null) {
            JsonObject metaLicense = license.getJsonObject("meta_ortolang-referential-json");
            if(metaLicense!=null) {
                this.addDcMultilingualField("rights", "fr", metaLicense.getString("label"));
            }
        }

        JsonArray linguisticSubjects = meta.getJsonArray("linguisticSubjects");
        if(linguisticSubjects!=null) {
            for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                this.addDcField("subject", "linguistic field: "+linguisticSubject.getString());
            }
        }
        JsonString linguisticDataType = meta.getJsonString("linguisticDataType");
        if(linguisticDataType!=null) {
            this.addDcField("type", "linguistic-type: "+linguisticDataType.getString());
        }
        JsonArray discourseTypes = meta.getJsonArray("discourseTypes");
        if(discourseTypes!=null) {
            for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                this.addDcField("type", "discourse-type: "+discourseType.getString());
            }
        }
        JsonString creationDate = meta.getJsonString("originDate");
        if(creationDate!=null) {
            this.addDcField("date", creationDate.getString());
        } else {
            JsonString publicationDate = meta.getJsonString("publicationDate");
            if(publicationDate!=null) {
                this.addDcField("date", publicationDate.getString());
            }
        }
    }
    
    private void fromCollection(JsonObject doc) {

    	JsonObject meta = doc.getJsonObject("meta_oai_dc");
    	
    	addDCElement("identifier", meta);
    	addDCElement("title", meta);
    	addDCElement("creator", meta);
    	addDCElement("subject", meta);
    	addDCElement("description", meta);
    	addDCElement("publisher", meta);
    	addDCElement("contributor", meta);
    	addDCElement("date", meta);
    	addDCElement("type", meta);
    	addDCElement("format", meta);
    	addDCElement("source", meta);
    	addDCElement("language", meta);
    	addDCElement("relation", meta);
    	addDCElement("coverage", meta);
    	addDCElement("rights", meta);
    }
    
    private void addDCElement(String elementName, JsonObject meta) {
    	if (meta.containsKey(elementName)) {
    		JsonArray titleArray = meta.getJsonArray(elementName);
            for(JsonObject title : titleArray.getValuesAs(JsonObject.class)) {
            	if (title.containsKey("lang")) {
            		this.addDcMultilingualField(elementName, 
                		title.getString("lang"), 
                		removeHTMLTag(title.getString("value")));
            	} else {
            		this.addDcField(elementName, removeHTMLTag(title.getString("value")));
            	}
            }
    	}
    }
    
    public static OAI_DC valueOf(JsonObject doc) {
        OAI_DC oaiDc = new OAI_DC();

        try {
        	if (doc.containsKey("meta_"+MetadataFormat.ITEM)) {        		
        		oaiDc.fromWorkspace(doc);
        	} else if (doc.containsKey("meta_"+MetadataFormat.OAI_DC)) {
        		oaiDc.fromCollection(doc);
        	}

        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Cannot parse JSON property", e);
        }

        return oaiDc;
    }

    public static String identifier(String wsalias) {
        return identifier(wsalias, null);
    }

    public static String identifier(String wsalias, String snapshotName) {
        return "http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
                + "/" + wsalias + ((snapshotName!=null) ? "/" + snapshotName : "");
    }

    protected static String person(JsonObject contributor) {
        JsonObject entityContributor = contributor.getJsonObject("entity");
        JsonObject entityMetaContributor = entityContributor.getJsonObject("meta_ortolang-referential-json");
        if(entityMetaContributor!=null) {
            JsonString lastname = entityMetaContributor.getJsonString("lastname");
            JsonString midname = entityMetaContributor.getJsonString("midname");
            JsonString firstname = entityMetaContributor.getJsonString("firstname");
            JsonString title = entityMetaContributor.getJsonString("title");
            JsonString acronym = null;

            if(entityMetaContributor.containsKey("organization")) {
                JsonObject entityOrganization = entityMetaContributor.getJsonObject("organization");
                JsonObject entityMetaOrganization = entityOrganization.getJsonObject("meta_ortolang-referential-json");

                acronym = entityMetaOrganization.getJsonString("acronym");
            }
            return (lastname!=null?lastname.getString():"")+(midname!=null?", "+midname.getString():"")+(firstname!=null?", "+firstname.getString():"")+(title!=null?" "+title.getString():"")+(acronym!=null?", "+acronym.getString():"");
        } else {
            JsonString lastname = entityContributor.getJsonString("lastname");
            JsonString midname = entityContributor.getJsonString("midname");
            JsonString firstname = entityContributor.getJsonString("firstname");
            JsonString title = entityContributor.getJsonString("title");
            JsonString acronym = null;

            if(entityContributor.containsKey("organization")) {
                JsonObject entityOrganization = entityContributor.getJsonObject("organization");
                JsonObject entityMetaOrganization = entityOrganization.getJsonObject("meta_ortolang-referential-json");

                acronym = entityMetaOrganization.getJsonString("acronym");
            }
            return (lastname!=null?lastname.getString():"")+(midname!=null?", "+midname.getString():"")+(firstname!=null?", "+firstname.getString():"")+(title!=null?" "+title.getString():"")+(acronym!=null?", "+acronym.getString():"");
        }
    }

    protected static String removeHTMLTag(String str) {
        return str.replaceAll("\\<.*?>","").replaceAll("\\&nbsp;"," ").replaceAll("\\&","");
    }
}
