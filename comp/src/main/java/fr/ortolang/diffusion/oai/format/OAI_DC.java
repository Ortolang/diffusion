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

public class OAI_DC extends DCXMLDocument {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());

    public OAI_DC() {
    	header = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">";
        footer = "</oai_dc:dc>";
    }

    public OAI_DC addDCElement(String elementName, JsonObject meta) {
    	if (meta.containsKey(elementName)) {
    		JsonArray titleArray = meta.getJsonArray(elementName);
            for(JsonObject title : titleArray.getValuesAs(JsonObject.class)) {
            	if (title.containsKey("lang")) {
            		this.addDcMultilingualField(elementName, 
                		title.getString("lang"), 
                		XMLDocument.removeHTMLTag(title.getString("value")));
            	} else {
            		this.addDcField(elementName, XMLDocument.removeHTMLTag(title.getString("value")));
            	}
            }
    	}
    	return this;
    }
    
    public static String identifier(String wsalias) {
        return identifier(wsalias, null);
    }

    public static String identifier(String wsalias, String snapshotName) {
        return "http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
                + "/" + wsalias + ((snapshotName!=null) ? "/" + snapshotName : "");
    }

    public static String person(JsonObject contributor) {
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
}
