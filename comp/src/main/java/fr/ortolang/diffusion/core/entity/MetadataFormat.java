package fr.ortolang.diffusion.core.entity;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({ @NamedQuery(name = "listMetadataFormat", query = "select f from MetadataFormat f"),
        @NamedQuery(name = "findMetadataFormatForName", query = "select f from MetadataFormat f where f.name = :name order by f.serial desc") })
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataFormat {

    public static final String ACL = "ortolang-acl-json";
    public static final String IGNORE = "ortolang-ignore-schema";
    public static final String ITEM = "ortolang-item-json";
    public static final String WORKSPACE = "ortolang-workspace-json";
    public static final String PID = "ortolang-pid-json";
    public static final String THUMB = "ortolang-thumb";
    public static final String TEMPLATE = "ortolang-template-json";
    public static final String OAI_DC = "oai_dc";
    public static final String OLAC = "olac";
    public static final String CMDI = "cmdi";
    public static final String OAI_DATACITE = "oai_datacite";
    public static final String TCOF = "tcof";
    // System metadata
    public static final String TRUSTRANK = "system-trustrank-json";
    public static final String RATING = "system-rating-json";
    // Metadata extraction
    public static final String AUDIO = "system-x-audio-json";
    public static final String VIDEO = "system-x-video-json";
    public static final String IMAGE = "system-x-image-json";
    public static final String XML = "system-x-xml-json";
    public static final String PDF = "system-x-pdf-json";
    public static final String TEXT = "system-x-text-json";
    public static final String OFFICE = "system-x-office-json";
    // Facile validator metadata
    public static final String FACILE_VALIDATOR = "system-facile-validator";



    @Id
    private String id;
    @Version
    private long version;
    private int serial;
    private String name;
    @Column(length = 2500)
    private String description;
    private long size;
    private String mimeType;
    private String schema;
    private String form;
    private boolean validationNeeded;
    private boolean indexable;

    public MetadataFormat() {
        serial = 1;
        validationNeeded = true;
        indexable = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public boolean isValidationNeeded() {
        return validationNeeded;
    }

    public void setValidationNeeded(boolean validationSkipped) {
        this.validationNeeded = validationSkipped;
    }

    public boolean isIndexable() {
        return indexable;
    }

    public void setIndexable(boolean indexable) {
        this.indexable = indexable;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MetadataFormat that = (MetadataFormat) o;

        if (size != that.size)
            return false;
        if (validationNeeded != that.validationNeeded)
            return false;
        if (indexable != that.indexable)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null)
            return false;
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null)
            return false;
        if (schema != null ? !schema.equals(that.schema) : that.schema != null)
            return false;
        return form != null ? form.equals(that.form) : that.form == null;

    }

    @Override public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (schema != null ? schema.hashCode() : 0);
        result = 31 * result + (form != null ? form.hashCode() : 0);
        result = 31 * result + (validationNeeded ? 1 : 0);
        result = 31 * result + (indexable ? 1 : 0);
        return result;
    }
}
