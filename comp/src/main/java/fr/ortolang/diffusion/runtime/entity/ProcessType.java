package fr.ortolang.diffusion.runtime.entity;

public class ProcessType {

	private String id;
	private String name;
	private String description;
	private boolean suspended;
	private int version;
	private String startForm;

	public ProcessType() {
		suspended = false;
		version = -1;
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

	public String getStartForm() {
		return startForm;
	}

	public void setStartForm(String startForm) {
		this.startForm = startForm;
	}

}
