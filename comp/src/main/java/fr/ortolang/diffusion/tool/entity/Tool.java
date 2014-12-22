package fr.ortolang.diffusion.tool.entity;

import javax.persistence.Column;
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
import fr.ortolang.diffusion.tool.ToolService;

@Entity
@NamedQueries({ 
	@NamedQuery(name = "findAllTools", query = "select t from Tool t") }
)
@SuppressWarnings("serial")
public class Tool extends OrtolangObject {

	public static final String OBJECT_TYPE = "tool";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	@Column(length = 2500)
	private String description;
	private String url;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String documentation;
	private String invokerClass;
	private String formConfig;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public String getInvokerClass() {
		return invokerClass;
	}

	public void setInvokerClass(String invokerClass) {
		this.invokerClass = invokerClass;
	}

	public String getFormConfig() {
		return formConfig;
	}

	public void setFormConfig(String formConfig) {
		this.formConfig = formConfig;
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
		return new OrtolangObjectIdentifier(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, id);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}


}
