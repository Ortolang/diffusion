package fr.ortolang.diffusion.api.referentiel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.referentiel.entity.OrganizationEntity;

@XmlRootElement(name = "group")
public class OrganizationEntityRepresentation {

    @XmlAttribute(name = "key")
    private String key;

    private String id;
	private String content;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public static OrganizationEntityRepresentation fromReferentielEntity(OrganizationEntity entity) {
		OrganizationEntityRepresentation representation = new OrganizationEntityRepresentation();
		representation.setId(entity.getId());
		representation.setKey(entity.getKey());
		representation.setContent(entity.getContent());
		
		return representation;
	}

}
