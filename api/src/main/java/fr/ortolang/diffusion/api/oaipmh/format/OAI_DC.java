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

public class OAI_DC {

    private static final Logger LOGGER = Logger.getLogger(OAI_DC.class.getName());
    protected static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");

    protected String header;
    protected String footer;
    protected List<XMLElement> fields;

    public OAI_DC() {
        header = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">";
        footer = "</oai_dc:dc>";
        fields = new ArrayList<XMLElement>();
    }

    public void addDcField(String name, String value) {
        fields.add(XMLElement.createDcElement(name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
        fields.add(XMLElement.createDcElement(name, value).withAttribute("xml:lang", lang));
    }

    protected static void writeField(StringBuilder buffer, XMLElement elem) {

        buffer.append("<").append(elem.getPrefixNamespace()).append(":").append(elem.getName());

        if(elem.getAttributes()!=null) {
            for(Map.Entry<String, String> attr : elem.getAttributes().entrySet()) {
                buffer.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
            }
        }

        if(elem.getValue()!=null) {
            buffer.append(">").append(elem.getValue()).append("</").append(elem.getPrefixNamespace()).append(":").append(elem.getName()).append(">");
        } else {
            buffer.append("/>");
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(header);

        for(XMLElement elem : fields) {
            writeField(buffer, elem);
        }

        buffer.append(footer);

        return buffer.toString();
    }

    public static OAI_DC valueOf(JsonObject doc) {
        OAI_DC oaiDc = new OAI_DC();

        oaiDc.addDcField("identifier", identifier(doc.getJsonObject("meta_ortolang-workspace-json").getString("wsalias")));

        JsonObject meta = doc.getJsonObject("meta_ortolang-item-json");

        try {
            JsonArray multilingualTitles = meta.getJsonArray("title");
            for(JsonObject multilingualTitle : multilingualTitles.getValuesAs(JsonObject.class)) {
                oaiDc.addDcMultilingualField("title", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
            }

            JsonArray multilingualDescriptions = meta.getJsonArray("description");
            for(JsonObject multilingualTitle : multilingualDescriptions.getValuesAs(JsonObject.class)) {
                oaiDc.addDcMultilingualField("description", multilingualTitle.getString("lang"), removeHTMLTag(multilingualTitle.getString("value")));
            }

            JsonArray corporaLanguages = meta.getJsonArray("corporaLanguages");
            if(corporaLanguages!=null) {
                for(JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
                    JsonObject metaLanguage = corporaLanguage.getJsonObject("meta_ortolang-referential-json");
                    JsonArray multilingualLabels = metaLanguage.getJsonArray("labels");

                    for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                        oaiDc.addDcMultilingualField("subject", label.getString("lang"), label.getString("value"));
                        oaiDc.addDcMultilingualField("language", label.getString("lang"), label.getString("value"));
                    }
                    oaiDc.addDcField("language", metaLanguage.getString("id"));
                }
            }

            JsonArray multilingualKeywords = meta.getJsonArray("keywords");
            if(multilingualKeywords!=null) {
                for(JsonObject multilingualKeyword : multilingualKeywords.getValuesAs(JsonObject.class)) {
                    oaiDc.addDcMultilingualField("subject", multilingualKeyword.getString("lang"), multilingualKeyword.getString("value"));
                }
            }

            JsonArray producers = meta.getJsonArray("producers");
            if(producers!=null) {
                for(JsonObject producer : producers.getValuesAs(JsonObject.class)) {
                    JsonObject metaOrganization = producer.getJsonObject("meta_ortolang-referential-json");

                    if(metaOrganization.containsKey("fullname")) {
                        oaiDc.addDcField("publisher", metaOrganization.getString("fullname"));
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
                        oaiDc.addDcField("contributor", person(contributor)+" ("+roleId+")");

                        if("author".equals(roleId)) {
                            oaiDc.addDcField("creator", person(contributor));
                        }
                    }
                }
            }

            JsonObject statusOfUse = meta.getJsonObject("statusOfUse");
            if(statusOfUse!=null) {
                JsonObject metaStatusOfUse = statusOfUse.getJsonObject("meta_ortolang-referential-json");
                String idStatusOfUse = metaStatusOfUse.getString("id");
                oaiDc.addDcField("rights", idStatusOfUse);

                JsonArray multilingualLabels = metaStatusOfUse.getJsonArray("labels");
                for(JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
                    oaiDc.addDcMultilingualField("rights", label.getString("lang"), label.getString("value"));
                }
            }
            JsonArray conditionsOfUse = meta.getJsonArray("conditionsOfUse");
            if(conditionsOfUse!=null) {
                for(JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
                    oaiDc.addDcMultilingualField("rights", label.getString("lang"), removeHTMLTag(label.getString("value")));
                }
            }

            JsonObject license = meta.getJsonObject("license");
            if(license!=null) {
                JsonObject metaLicense = license.getJsonObject("meta_ortolang-referential-json");
                if(metaLicense!=null) {
                    oaiDc.addDcMultilingualField("rights", "fr", metaLicense.getString("label"));
                }
            }

            JsonArray linguisticSubjects = meta.getJsonArray("linguisticSubjects");
            if(linguisticSubjects!=null) {
                for(JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
                    oaiDc.addDcField("subject", "linguistic field: "+linguisticSubject.getString());
                }
            }
            JsonString linguisticDataType = meta.getJsonString("linguisticDataType");
            if(linguisticDataType!=null) {
                oaiDc.addDcField("type", "linguistic-type: "+linguisticDataType.getString());
            }
            JsonArray discourseTypes = meta.getJsonArray("discourseTypes");
            if(discourseTypes!=null) {
                for(JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
                    oaiDc.addDcField("type", "discourse-type: "+discourseType.getString());
                }
            }
            JsonString creationDate = meta.getJsonString("originDate");
            if(creationDate!=null) {
                oaiDc.addDcField("date", creationDate.getString());
            } else {
                JsonString publicationDate = meta.getJsonString("publicationDate");
                if(publicationDate!=null) {
                    oaiDc.addDcField("date", publicationDate.getString());
                }
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
