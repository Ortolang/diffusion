package fr.ortolang.diffusion.api.rest.runtime;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.runtime.entity.ProcessType;

@XmlRootElement(name = "process-type")
public class ProcessTypeRepresentation {

	@XmlAttribute
	private String id;
	private String name;
	private String friendlyName;
	private String description;
	private boolean suspended;
	private int version;
	private String form;

	public ProcessTypeRepresentation() {
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

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String form) {
		this.form = form;
	}

	public static ProcessTypeRepresentation fromProcessType(ProcessType type) {
		ProcessTypeRepresentation representation = new ProcessTypeRepresentation();
		representation.setId(type.getId());
		representation.setName(type.getName());
		representation.setDescription(type.getDescription());
		representation.setSuspended(type.isSuspended());
		representation.setVersion(type.getVersion());
		representation.setForm(type.getStartForm());
		return representation;
	}

}
