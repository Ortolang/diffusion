package fr.ortolang.diffusion.referential.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.referential.ReferentialService;

@Entity
@NamedQueries({
	@NamedQuery(name = "findAllEntitiesWithType", query = "SELECT r FROM ReferentialEntity r WHERE r.type = :type") }
)
@SuppressWarnings("serial")
public class ReferentialEntity extends OrtolangObject {

	public static final String OBJECT_TYPE = "entity";

	public static final String CONTENT_TEXT = "TEXT";
    public static final String CONTENT_TYPE = "TYPE";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;

	@Enumerated(EnumType.STRING)
	private ReferentialEntityType type;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String content;
	private Long boost;

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

	public ReferentialEntityType getType() {
		return type;
	}

	public void setType(ReferentialEntityType type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public Long getBoost() {
		return boost;
	}

	public void setBoost(Long boost) {
		this.boost = boost;
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
		return new OrtolangObjectIdentifier(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, id);
	}

}
