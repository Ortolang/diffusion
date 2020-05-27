package fr.ortolang.diffusion.archive.facile.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "validator", namespace = "http://facile.cines.fr")
public class Validator {
    
    private String fileName;
    private Boolean valid;
    private Boolean wellFormed;
    private Boolean archivable;
    private String md5sum;
    private String sha256sum;
    private Integer size;
    private String message;

    @XmlElement(name="fileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @XmlElement(name = "valid")
    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    @XmlElement(name = "wellFormed")
    public Boolean getWellFormed() {
        return wellFormed;
    }

    public void setWellFormed(Boolean wellFormed) {
        this.wellFormed = wellFormed;
    }
    
    @XmlElement(name = "archivable")
    public Boolean getArchivable() {
        return archivable;
    }

    public void setArchivable(Boolean archivable) {
        this.archivable = archivable;
    }

    @XmlElement(name = "md5sum")
    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    @XmlElement(name = "sha256sum")
    public String getSha256sum() {
        return sha256sum;
    }

    public void setSha256sum(String sha256sum) {
        this.sha256sum = sha256sum;
    }

    @XmlElement(name = "size")
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @XmlElement(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}