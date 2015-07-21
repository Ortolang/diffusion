package fr.ortolang.diffusion.api.referentiel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.referentiel.entity.ReferentielEntity;
import fr.ortolang.diffusion.referentiel.entity.ReferentielType;

@XmlRootElement(name = "group")
public class ReferentielEntityRepresentation {

    @XmlAttribute(name = "key")
    private String key;

    private String id;
    private String name;
	private ReferentielType type;
	private String content;
	private String status;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ReferentielType getType() {
		return type;
	}

	public void setType(ReferentielType type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public static ReferentielEntityRepresentation fromReferentielEntity(ReferentielEntity entity) {
		ReferentielEntityRepresentation representation = new ReferentielEntityRepresentation();
		representation.setId(entity.getId());
		representation.setKey(entity.getKey());
		representation.setName(entity.getName());
		representation.setContent(entity.getContent());
		representation.setStatus(entity.getStatus());
		
		return representation;
	}

}
