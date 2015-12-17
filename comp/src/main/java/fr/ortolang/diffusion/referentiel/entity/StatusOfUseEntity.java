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
	@NamedQuery(name = "findAllStatusOfUseEntities", query = "select r from StatusOfUseEntity r") }
)
@SuppressWarnings("serial")
public class StatusOfUseEntity extends OrtolangObject {

	public static final String OBJECT_TYPE = "statusofuse";

	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String content;

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

	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String getObjectName() {
		return getKey();
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(ReferentielService.SERVICE_NAME, StatusOfUseEntity.OBJECT_TYPE, id);
	}

}
