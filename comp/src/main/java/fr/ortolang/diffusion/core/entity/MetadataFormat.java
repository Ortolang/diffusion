package fr.ortolang.diffusion.core.entity;

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
    public static final String ITEM = "ortolang-item-json";
    public static final String WORKSPACE = "ortolang-workspace-json";
    public static final String PID = "ortolang-pid-json";
    public static final String THUMB = "thumbnail";
    public static final String TEMPLATE = "ortolang-template-json";


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
        if (form != null ? !form.equals(that.form) : that.form != null)
            return false;

        return true;
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
