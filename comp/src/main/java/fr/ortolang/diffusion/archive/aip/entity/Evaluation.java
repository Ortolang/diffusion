package fr.ortolang.diffusion.archive.aip.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "evaluation")
public class Evaluation {
    private String DUA;
    private String traitement;
    private String dateDebut;
    
    @XmlElement(name="DUA")
    public String getDUA() {
        return DUA;
    }
    public void setDUA(String dUA) {
        DUA = dUA;
    }
    @XmlElement(name="traitement")
    public String getTraitement() {
        return traitement;
    }
    public void setTraitement(String traitement) {
        this.traitement = traitement;
    }
    @XmlElement(name="dateDebut")
    public String getDateDebut() {
        return dateDebut;
    }
    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }
}
