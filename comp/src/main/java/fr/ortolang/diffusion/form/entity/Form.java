package fr.ortolang.diffusion.form.entity;

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
import fr.ortolang.diffusion.form.FormService;

@Entity
@NamedQueries({ 
	@NamedQuery(name = "findAllForms", query = "select f from Form f") }
)
@SuppressWarnings("serial")
public class Form extends OrtolangObject {

	public static final String OBJECT_TYPE = "form";

	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String definition;

	public Form() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Transient
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

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(FormService.SERVICE_NAME, Form.OBJECT_TYPE, id);
	}

}