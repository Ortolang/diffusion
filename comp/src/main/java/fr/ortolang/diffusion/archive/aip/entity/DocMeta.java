package fr.ortolang.diffusion.archive.aip.entity;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamReader;

@XmlRootElement(name = "DocMeta")
public class DocMeta {
    /**
     * Identifier ARK From DocDC.
     */
    private String identifier;
    private String dateArchivage;
    private String identifiantDocPac;
    private String identifiantDocProducteur;
    private List<DocRelation> docRelation;
    private Evaluation evaluation;
    private Communicabilite communicabilite;
    private String noteDocument;
    private String serviceVersant;
    private String planClassement;
    private String identifiantVersement;
    private String projet;
    private String sortFinal;
    private List<String> structureDocument;
    private String version;
    private String versionPrecedente;

    @XmlElement(name="identifier")
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    @XmlElement(name="dateArchivage")
	public String getDateArchivage() {
		return dateArchivage;
	}
	public void setDateArchivage(String dateArchivage) {
		this.dateArchivage = dateArchivage;
	}
    @XmlElement(name="identifiantDocPac")
	public String getIdentifiantDocPac() {
		return identifiantDocPac;
	}
	public void setIdentifiantDocPac(String identifiantDocPac) {
		this.identifiantDocPac = identifiantDocPac;
	}
    @XmlElement(name="identifiantDocProducteur")
	public String getIdentifiantDocProducteur() {
		return identifiantDocProducteur;
	}
	public void setIdentifiantDocProducteur(String identifiantDocProducteur) {
		this.identifiantDocProducteur = identifiantDocProducteur;
	}
    @XmlElement(name="docRelation")
	public List<DocRelation> getDocRelation() {
		return docRelation;
	}
	public void setDocRelation(List<DocRelation> docRelation) {
		this.docRelation = docRelation;
	}
    @XmlElement(name="evaluation")
	public Evaluation getEvaluation() {
		return evaluation;
	}
	public void setEvaluation(Evaluation evaluation) {
		this.evaluation = evaluation;
	}
    @XmlElement(name="communicabilite")
	public Communicabilite getCommunicabilite() {
		return communicabilite;
	}
	public void setCommunicabilite(Communicabilite communicabilite) {
		this.communicabilite = communicabilite;
	}
    @XmlElement(name="noteDocument")
	public String getNoteDocument() {
		return noteDocument;
	}
	public void setNoteDocument(String noteDocument) {
		this.noteDocument = noteDocument;
	}
    @XmlElement(name="serviceVersant")
	public String getServiceVersant() {
		return serviceVersant;
	}
	public void setServiceVersant(String serviceVersant) {
		this.serviceVersant = serviceVersant;
	}
    @XmlElement(name="planClassement")
	public String getPlanClassement() {
		return planClassement;
	}
	public void setPlanClassement(String planClassement) {
		this.planClassement = planClassement;
	}
    @XmlElement(name="identifiantVersement")
	public String getIdentifiantVersement() {
		return identifiantVersement;
	}
	public void setIdentifiantVersement(String identifiantVersement) {
		this.identifiantVersement = identifiantVersement;
	}
    @XmlElement(name="projet")
	public String getProjet() {
		return projet;
	}
	public void setProjet(String projet) {
		this.projet = projet;
	}
    @XmlElement(name="sortFinal")
	public String getSortFinal() {
		return sortFinal;
	}
	public void setSortFinal(String sortFinal) {
		this.sortFinal = sortFinal;
	}
    @XmlElement(name="structureDocument")
	public List<String> getStructureDocument() {
		return structureDocument;
	}
	public void setStructureDocument(List<String> structureDocument) {
		this.structureDocument = structureDocument;
	}
    @XmlElement(name="version")
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
    @XmlElement(name="versionPrecedente")
	public String getVersionPrecedente() {
		return versionPrecedente;
	}
	public void setVersionPrecedente(String versionPrecedente) {
		this.versionPrecedente = versionPrecedente;
	}
    public static DocMeta fromXMLStreamReader(XMLStreamReader reader) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(DocMeta.class);
        Unmarshaller um = ctx.createUnmarshaller();
        return (DocMeta) um.unmarshal(reader);
    }
}
