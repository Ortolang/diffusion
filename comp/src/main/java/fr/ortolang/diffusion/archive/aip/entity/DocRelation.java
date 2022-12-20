package fr.ortolang.diffusion.archive.aip.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DocRelation")
public class DocRelation {
    private String typeRelation;
    private String sourceRelation;
    private String identifiantSourceRelation;

    @XmlElement(name="typeRelation")
    public String getTypeRelation() {
        return typeRelation;
    }
    public void setTypeRelation(String typeRelation) {
        this.typeRelation = typeRelation;
    }
    @XmlElement(name="sourceRelation")
    public String getSourceRelation() {
        return sourceRelation;
    }
    public void setSourceRelation(String sourceRelation) {
        this.sourceRelation = sourceRelation;
    }
    @XmlElement(name="identifiantSourceRelation")
    public String getIdentifiantSourceRelation() {
        return identifiantSourceRelation;
    }
    public void setIdentifiantSourceRelation(String identifiantSourceRelation) {
        this.identifiantSourceRelation = identifiantSourceRelation;
    }


}
