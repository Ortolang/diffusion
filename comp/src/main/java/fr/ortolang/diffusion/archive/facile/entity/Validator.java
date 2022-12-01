package fr.ortolang.diffusion.archive.facile.entity;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "validator")
public class Validator {
    
    private String fileName;
    private Boolean valid;
    private Boolean wellFormed;
    private Boolean archivable;
    private String md5sum;
    private String sha256sum;
    private Long size;
    private String format;
    private String version;
    private String encoding;
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
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @XmlElement(name = "format")
    public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	@XmlElement(name = "version")
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@XmlElement(name = "encoding")
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@XmlElement(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{valid: ").append(valid)
            .append(",fileName: ").append(fileName)
            .append(",wellFormed: ").append(wellFormed)
            .append(",archivable: ").append(archivable)
            .append(",md5sum: ").append(md5sum)
            .append(",sha256sum: ").append(sha256sum)
            .append(",size: ").append(size)
            .append(",format: ").append(format)
            .append(",version: ").append(version)
            .append(",encoding: ").append(encoding)
            .append(",message: ").append(message)
            .append("}");
        return builder.toString();
    }

    public static Validator fromXML(String xml) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(Validator.class);
        Unmarshaller um = ctx.createUnmarshaller();
        return (Validator) um.unmarshal(new StringReader(xml));
    }
}