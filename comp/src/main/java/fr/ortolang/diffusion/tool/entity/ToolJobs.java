package fr.ortolang.diffusion.tool.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import javax.persistence.Version;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.tool.ToolService;

@Entity
@NamedQueries({ 
	@NamedQuery(name = "findAllJobs", query = "select t from ToolJobs t") }
)
@SuppressWarnings("serial")
public class ToolJobs extends OrtolangObject {

	public static final String OBJECT_TYPE = "toolJob";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	private ToolJobStatus status;

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

	public ToolJobStatus getStatus() {
		return status;
	}

	public void setStatus(ToolJobStatus status) {
		this.status = status;
	}

	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(ToolService.SERVICE_NAME, ToolJobs.OBJECT_TYPE, id);
	}

}
