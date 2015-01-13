package fr.ortolang.diffusion.api.rest.tool;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.tool.entity.ToolJobStatus;

@XmlRootElement(name = "toolJobs")
public class ToolJobsRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private ToolJobStatus status;

	public ToolJobsRepresentation() {}

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

	public void setStatus(String status) {
		this.status = ToolJobStatus.valueOf(status);
	}
}
