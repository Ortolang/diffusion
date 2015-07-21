package fr.ortolang.diffusion.referentiel.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.referentiel.ReferentielService;

@Entity
@NamedQueries({ 
	@NamedQuery(name = "findAllReferentielEntities", query = "select r from ReferentielEntity r") }
)
@SuppressWarnings("serial")
public class ReferentielEntity extends OrtolangObject {

	public static final String OBJECT_TYPE = "ReferentielEntity";

	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String content;
	private String status;
	private ReferentielType type;
	
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
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public ReferentielType getType() {
		return type;
	}
	public void setType(ReferentielType type) {
		this.type = type;
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public String getObjectName() {
		return getKey();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(ReferentielService.SERVICE_NAME, ReferentielEntity.OBJECT_TYPE, id);
	}

	
}
