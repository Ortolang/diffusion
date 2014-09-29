package fr.ortolang.diffusion.api.rest.workflow;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class WorkflowDefinitionFormRepresentation {

	@FormParam("name")
	@PartType("text/plain")
	private String name = null;

	@FormParam("content")
	@PartType("application/octet-stream")
	private InputStream content = null;

	public WorkflowDefinitionFormRepresentation() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InputStream getContent() {
		return content;
	}

	public void setContent(InputStream content) {
		this.content = content;
	}

}
