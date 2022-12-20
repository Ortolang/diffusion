package fr.ortolang.diffusion.archive.aip.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Communicabilite")
public class Communicabilite {
    private String code;
    private String dateDebut;

    @XmlElement(name="code")
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    @XmlElement(name="dateDebut")
    public String getDateDebut() {
        return dateDebut;
    }
    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }
}
